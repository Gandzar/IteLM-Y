package com.example.download

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.example.YMusicApplication
import com.example.data.DownloadStatus
import com.example.debug.AppDiagnostics
import com.example.notify.AppNotifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class DownloadForegroundService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var jobsWatch: Job? = null
    private var isForegroundStarted = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        AppNotifications.ensureChannels(this)
        AppDiagnostics.setServiceRunning("DownloadForegroundService", true)
        observeDownloads()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val manager = runCatching { YMusicApplication.instance.downloadManager }.getOrNull()
        val active = manager?.jobs?.value?.filter {
            it.status == DownloadStatus.QUEUED ||
                it.status == DownloadStatus.DOWNLOADING ||
                it.status == DownloadStatus.CONVERTING
        }.orEmpty()

        val current = active.firstOrNull()
        val notif = AppNotifications.buildDownloadProgressNotification(
            context = this,
            title = current?.track?.title ?: "Menyiapkan unduhan…",
            progress = current?.progress ?: 0,
            activeCount = active.size.coerceAtLeast(1),
        )
        if (!isForegroundStarted) {
            isForegroundStarted = true
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        AppNotifications.NOTIF_ID_DOWNLOAD_PROGRESS,
                        notif,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
                    )
                } else {
                    startForeground(AppNotifications.NOTIF_ID_DOWNLOAD_PROGRESS, notif)
                }
            }
        } else if (active.isNotEmpty()) {
            getSystemService(NotificationManager::class.java)
                ?.notify(AppNotifications.NOTIF_ID_DOWNLOAD_PROGRESS, notif)
        }

        if (active.isEmpty()) {
            stopSelfClean()
            return START_NOT_STICKY
        }
        return START_NOT_STICKY
    }

    private fun observeDownloads() {
        val manager = runCatching { YMusicApplication.instance.downloadManager }.getOrNull() ?: return
        jobsWatch?.cancel()
        jobsWatch = scope.launch {
            manager.jobs.collect { list ->
                val active = list.filter {
                    it.status == DownloadStatus.QUEUED ||
                        it.status == DownloadStatus.DOWNLOADING ||
                        it.status == DownloadStatus.CONVERTING
                }
                if (active.isEmpty()) {
                    if (isForegroundStarted) stopSelfClean()
                } else {
                    val current = active.first()
                    val notif = AppNotifications.buildDownloadProgressNotification(
                        context = this@DownloadForegroundService,
                        title = current.track.title,
                        progress = current.progress,
                        activeCount = active.size,
                    )
                    if (!isForegroundStarted) {
                        isForegroundStarted = true
                        runCatching {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                startForeground(
                                    AppNotifications.NOTIF_ID_DOWNLOAD_PROGRESS,
                                    notif,
                                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
                                )
                            } else {
                                startForeground(AppNotifications.NOTIF_ID_DOWNLOAD_PROGRESS, notif)
                            }
                        }
                    } else {
                        getSystemService(NotificationManager::class.java)
                            ?.notify(AppNotifications.NOTIF_ID_DOWNLOAD_PROGRESS, notif)
                    }
                }
            }
        }
    }

    private fun stopSelfClean() {
        isForegroundStarted = false
        runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
        stopSelf()
    }

    override fun onDestroy() {
        isForegroundStarted = false
        jobsWatch?.cancel()
        scope.cancel()
        AppDiagnostics.setServiceRunning("DownloadForegroundService", false)
        super.onDestroy()
    }

    companion object {
        fun sync(context: Context) {
            val intent = Intent(context, DownloadForegroundService::class.java)
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }
        }
    }
}
