package com.example.share

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.DownloadJob
import com.example.data.Track
import com.example.debug.AppDiagnostics
import java.io.File

object ShareHelper {
    fun buildTrackShareUrl(track: Track): String {
        val vid = track.videoId
        return when {
            !vid.isNullOrBlank() -> "https://music.youtube.com/watch?v=$vid"
            !track.url.isNullOrBlank() -> track.url
            !track.playlistBrowseId.isNullOrBlank() ->
                "https://music.youtube.com/playlist?list=${track.playlistBrowseId.removePrefix("VL")}"
            else -> track.title
        }
    }

    fun shareTrackLink(context: Context, track: Track) {
        val url = buildTrackShareUrl(track)
        val text = buildString {
            append(track.title)
            if (track.artist.isNotBlank()) append(" — ${track.artist}")
            if (url.startsWith("http")) {
                append("\n")
                append(url)
            }
        }
        shareText(context, text, "Bagikan ${track.title}")
    }

    fun shareText(context: Context, text: String, chooserTitle: String = "Bagikan tautan") {
        runCatching {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            val chooser = Intent.createChooser(sendIntent, chooserTitle).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            AppDiagnostics.info("Share", "Shared text/link")
        }.onFailure {
            AppDiagnostics.warn("Share", "Share text failed: ${it.message}")
        }
    }

    fun shareDownloadedJob(context: Context, job: DownloadJob): Boolean {
        val uriString = job.contentUri?.takeIf { it.isNotBlank() }
            ?: job.filePath?.takeIf { it.startsWith("/") }
        if (uriString.isNullOrBlank()) {
            shareTrackLink(context, job.track)
            return false
        }
        return runCatching {
            val uri: Uri = if (uriString.startsWith("/")) {
                val file = File(uriString)
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            } else {
                Uri.parse(uriString)
            }
            val mime = job.mimeType?.takeIf { it.isNotBlank() } ?: "*/*"
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = mime
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "${job.track.title} — ${job.track.artist}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(sendIntent, "Bagikan file ${job.track.title}").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(chooser)
            AppDiagnostics.info("Share", "Shared local file: ${job.track.title}")
            true
        }.getOrElse {
            AppDiagnostics.warn("Share", "File share failed, falling back to link: ${it.message}")
            shareTrackLink(context, job.track)
            false
        }
    }
}
