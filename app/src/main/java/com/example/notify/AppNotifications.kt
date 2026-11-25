package com.example.notify

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.DownloadJob
import com.example.debug.DebugActivity

object AppNotifications {
    const val CHANNEL_PLAYBACK = "ymusic_playback"
    const val CHANNEL_DOWNLOADS = "ymusic_downloads"
    const val CHANNEL_DEBUG = "ymusic_debug"

    const val NOTIF_ID_PLAYBACK = 3101
    const val NOTIF_ID_DOWNLOAD_PROGRESS = 3201
    const val NOTIF_ID_DEBUG = 3301

    const val EXTRA_NAV_ROUTE = "ymusic_nav_route"
    const val EXTRA_JOB_ID = "ymusic_job_id"
    const val EXTRA_PREVIEW_URI = "ymusic_preview_uri"
    const val EXTRA_PREVIEW_TITLE = "ymusic_preview_title"
    const val EXTRA_PREVIEW_MIME = "ymusic_preview_mime"

    const val ROUTE_PLAYER = "player"
    const val ROUTE_DOWNLOAD_QUEUE = "download_queue"
    const val ROUTE_DOWNLOAD_PREVIEW = "download_preview"
    const val ROUTE_SPATIAL = "spatial"
    const val ROUTE_DEBUG = "debug"

    private const val REQ_PLAYER = 4101
    private const val REQ_DOWNLOAD_QUEUE = 4201
    private const val REQ_SPATIAL = 4301
    private const val REQ_DEBUG = 4401

    fun ensureChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL_PLAYBACK) == null) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_PLAYBACK,
                    "Pemutar Musik & 8D",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = "Kontrol pemutaran audio latar belakang dan mode 8D"
                },
            )
        }
        if (nm.getNotificationChannel(CHANNEL_DOWNLOADS) == null) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_DOWNLOADS,
                    "Unduhan Media",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = "Progres dan status selesai unduhan MP3/MP4"
                },
            )
        }
        if (nm.getNotificationChannel(CHANNEL_DEBUG) == null) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_DEBUG,
                    "Diagnostik & Debug",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = "Notifikasi uji dan diagnostik RAM / log"
                },
            )
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun createMainRouteIntent(
        context: Context,
        route: String,
        jobId: String? = null,
        previewUri: String? = null,
        previewTitle: String? = null,
        previewMime: String? = null,
    ): Intent {
        return Intent(context, MainActivity::class.java).apply {
            action = "com.example.ACTION_OPEN_ROUTE_$route"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_NAV_ROUTE, route)
            if (!jobId.isNullOrBlank()) putExtra(EXTRA_JOB_ID, jobId)
            if (!previewUri.isNullOrBlank()) putExtra(EXTRA_PREVIEW_URI, previewUri)
            if (!previewTitle.isNullOrBlank()) putExtra(EXTRA_PREVIEW_TITLE, previewTitle)
            if (!previewMime.isNullOrBlank()) putExtra(EXTRA_PREVIEW_MIME, previewMime)
        }
    }

    fun createMainRoutePendingIntent(
        context: Context,
        route: String,
        requestCode: Int = when (route) {
            ROUTE_PLAYER -> REQ_PLAYER
            ROUTE_DOWNLOAD_QUEUE -> REQ_DOWNLOAD_QUEUE
            ROUTE_SPATIAL -> REQ_SPATIAL
            else -> REQ_PLAYER
        },
        jobId: String? = null,
        previewUri: String? = null,
        previewTitle: String? = null,
        previewMime: String? = null,
    ): PendingIntent {
        val intent = createMainRouteIntent(
            context = context,
            route = route,
            jobId = jobId,
            previewUri = previewUri,
            previewTitle = previewTitle,
            previewMime = previewMime,
        )
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun createDebugPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, DebugActivity::class.java).apply {
            action = "com.example.ACTION_OPEN_DEBUG"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_NAV_ROUTE, ROUTE_DEBUG)
        }
        return PendingIntent.getActivity(
            context,
            REQ_DEBUG,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun buildDownloadProgressNotification(
        context: Context,
        title: String,
        progress: Int,
        activeCount: Int,
    ): Notification {
        ensureChannels(context)
        val openQueueIntent = createMainRoutePendingIntent(context, ROUTE_DOWNLOAD_QUEUE, REQ_DOWNLOAD_QUEUE)
        val sub = if (activeCount > 1) "$activeCount unduhan aktif · $progress%" else "$progress%"
        return NotificationCompat.Builder(context, CHANNEL_DOWNLOADS)
            .setSmallIcon(R.drawable.ic_tile_8d)
            .setContentTitle("Mengunduh: $title")
            .setContentText(sub)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setProgress(100, progress.coerceIn(0, 100), progress <= 5)
            .setContentIntent(openQueueIntent)
            .build()
    }

    fun notifyDownloadFinished(context: Context, job: DownloadJob) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)
        val notifId = 5000 + (job.id.hashCode() and 0x0FFF)
        val openPreviewIntent = createMainRoutePendingIntent(
            context = context,
            route = ROUTE_DOWNLOAD_PREVIEW,
            requestCode = notifId,
            jobId = job.id,
            previewUri = job.contentUri ?: job.filePath,
            previewTitle = job.track.title,
            previewMime = job.mimeType,
        )
        val notif = NotificationCompat.Builder(context, CHANNEL_DOWNLOADS)
            .setSmallIcon(R.drawable.ic_tile_8d)
            .setContentTitle("Selesai: ${job.track.title}")
            .setContentText("Ketuk untuk pratinjau file · ${job.filePath ?: job.format.label}")
            .setAutoCancel(true)
            .setContentIntent(openPreviewIntent)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(notifId, notif) }
    }

    fun notifyDownloadFailed(context: Context, job: DownloadJob) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)
        val notifId = 6000 + (job.id.hashCode() and 0x0FFF)
        val openQueueIntent = createMainRoutePendingIntent(
            context = context,
            route = ROUTE_DOWNLOAD_QUEUE,
            requestCode = notifId,
            jobId = job.id,
        )
        val notif = NotificationCompat.Builder(context, CHANNEL_DOWNLOADS)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("Gagal unduh: ${job.track.title}")
            .setContentText(job.error ?: "Ketuk untuk melihat antrean")
            .setAutoCancel(true)
            .setContentIntent(openQueueIntent)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(notifId, notif) }
    }

    fun notifyDebugDiagnostic(context: Context, title: String, summary: String) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)
        val debugIntent = createDebugPendingIntent(context)
        val notif = NotificationCompat.Builder(context, CHANNEL_DEBUG)
            .setSmallIcon(R.drawable.ic_tile_8d)
            .setContentTitle(title)
            .setContentText(summary)
            .setAutoCancel(true)
            .setContentIntent(debugIntent)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(NOTIF_ID_DEBUG, notif) }
    }
}
