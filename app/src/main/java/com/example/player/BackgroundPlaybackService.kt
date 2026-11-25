package com.example.player

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.YMusicApplication
import com.example.debug.AppDiagnostics
import com.example.notify.AppNotifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class BackgroundPlaybackService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var stateJob: Job? = null
    private var isForegroundStarted = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        AppNotifications.ensureChannels(this)
        AppDiagnostics.setServiceRunning("BackgroundPlaybackService", true)
        observePlayerState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val player = runCatching { YMusicApplication.instance.player }.getOrNull()
        val initialState = player?.state?.value ?: PlayerState()

        // Always fulfill startForegroundService() contract immediately on every start command
        promoteOrUpdate(initialState)

        when (intent?.action) {
            ACTION_TOGGLE -> {
                player?.toggle()
                val updated = player?.state?.value ?: initialState
                if (updated.track == null || updated.mode == PlayerMode.NONE) {
                    stopSelfClean()
                } else {
                    promoteOrUpdate(updated)
                }
            }
            ACTION_PREV -> {
                player?.playPrevious()
                val updated = player?.state?.value ?: initialState
                if (updated.track == null || updated.mode == PlayerMode.NONE) {
                    stopSelfClean()
                } else {
                    promoteOrUpdate(updated)
                }
            }
            ACTION_NEXT -> {
                player?.playNext()
                val updated = player?.state?.value ?: initialState
                if (updated.track == null || updated.mode == PlayerMode.NONE) {
                    stopSelfClean()
                } else {
                    promoteOrUpdate(updated)
                }
            }
            ACTION_STOP -> {
                player?.stop()
                stopSelfClean()
            }
            else -> {
                if (initialState.track == null || initialState.mode == PlayerMode.NONE) {
                    stopSelfClean()
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun observePlayerState() {
        val player = runCatching { YMusicApplication.instance.player }.getOrNull() ?: return
        stateJob?.cancel()
        stateJob = scope.launch {
            player.state.collect { st ->
                if (st.track == null || st.mode == PlayerMode.NONE) {
                    if (isForegroundStarted) {
                        stopSelfClean()
                    }
                } else {
                    promoteOrUpdate(st)
                }
            }
        }
    }

    private fun promoteOrUpdate(state: PlayerState) {
        val notif = buildNotification(state)
        if (!isForegroundStarted) {
            isForegroundStarted = true
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        AppNotifications.NOTIF_ID_PLAYBACK,
                        notif,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
                    )
                } else {
                    startForeground(AppNotifications.NOTIF_ID_PLAYBACK, notif)
                }
            }.onFailure { err ->
                AppDiagnostics.error("PlayerService", "startForeground failed: ${err.message}")
            }
        } else {
            val nm = getSystemService(android.app.NotificationManager::class.java)
            nm?.notify(AppNotifications.NOTIF_ID_PLAYBACK, notif)
        }
    }

    private fun buildNotification(state: PlayerState): Notification {
        val track = state.track
        val title = track?.title?.ifBlank { "YMusic Player" } ?: "YMusic Player"
        val sleepSuffix = when {
            state.sleepAfterCurrentTrack -> " · Timer: Akhir lagu"
            state.sleepTimerRemainingSec != null && state.sleepTimerRemainingSec > 0L ->
                " · Timer ${state.sleepTimerRemainingSec / 60}m"
            else -> ""
        }
        val subtitle = buildString {
            append(track?.artist.orEmpty())
            if (state.audioCodecLabel.isNotBlank()) {
                if (isNotEmpty()) append(" · ")
                append(state.audioCodecLabel)
            }
            append(sleepSuffix)
        }.ifBlank { "Audio 8D Aktif" }

        val openPlayerIntent = AppNotifications.createMainRoutePendingIntent(
            context = this,
            route = AppNotifications.ROUTE_PLAYER,
        )
        val prevIntent = PendingIntent.getService(
            this,
            50,
            Intent(this, BackgroundPlaybackService::class.java).setAction(ACTION_PREV),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val toggleIntent = PendingIntent.getService(
            this,
            51,
            Intent(this, BackgroundPlaybackService::class.java).setAction(ACTION_TOGGLE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val nextIntent = PendingIntent.getService(
            this,
            53,
            Intent(this, BackgroundPlaybackService::class.java).setAction(ACTION_NEXT),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = PendingIntent.getService(
            this,
            52,
            Intent(this, BackgroundPlaybackService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val toggleIcon = if (state.playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val toggleLabel = if (state.playing) "Jeda" else "Putar"

        return NotificationCompat.Builder(this, AppNotifications.CHANNEL_PLAYBACK)
            .setSmallIcon(R.drawable.ic_tile_8d)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setOngoing(state.playing || state.resolvingStream)
            .setOnlyAlertOnce(true)
            .setContentIntent(openPlayerIntent)
            .addAction(android.R.drawable.ic_media_previous, "Sebelumnya", prevIntent)
            .addAction(toggleIcon, toggleLabel, toggleIntent)
            .addAction(android.R.drawable.ic_media_next, "Berikutnya", nextIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Tutup", stopIntent)
            .build()
    }

    private fun stopSelfClean() {
        isForegroundStarted = false
        runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
        stopSelf()
    }

    override fun onDestroy() {
        isForegroundStarted = false
        stateJob?.cancel()
        scope.cancel()
        AppDiagnostics.setServiceRunning("BackgroundPlaybackService", false)
        super.onDestroy()
    }

    companion object {
        const val ACTION_SYNC = "com.example.player.ACTION_SYNC"
        const val ACTION_TOGGLE = "com.example.player.ACTION_TOGGLE"
        const val ACTION_PREV = "com.example.player.ACTION_PREV"
        const val ACTION_NEXT = "com.example.player.ACTION_NEXT"
        const val ACTION_STOP = "com.example.player.ACTION_STOP"

        fun sync(context: Context) {
            val state = runCatching { YMusicApplication.instance.player.state.value }.getOrNull()
            val intent = Intent(context, BackgroundPlaybackService::class.java).setAction(ACTION_SYNC)
            if (state == null || state.track == null || state.mode == PlayerMode.NONE) {
                runCatching { context.stopService(intent) }
                return
            }
            runCatching {
                ContextCompatHelper.startForegroundSafe(context, intent)
            }.onFailure { err ->
                AppDiagnostics.warn("PlayerService", "Unable to start BackgroundPlaybackService: ${err.message}")
            }
        }
    }
}

private object ContextCompatHelper {
    fun startForegroundSafe(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
}
