package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.data.PreviewKind
import com.example.data.PreviewTarget
import com.example.debug.AppDiagnostics
import com.example.engine.InnerTubeClient
import com.example.engine.MediaScraper
import com.example.engine.PlatformDetector
import com.example.ui.theme.YtCard
import com.example.ui.theme.YtDivider
import com.example.ui.theme.YtElevated
import com.example.ui.theme.YtOrange
import com.example.ui.theme.YtTextMuted
import com.example.ui.theme.YtTextPrimary
import com.example.ui.theme.YtTextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(UnstableApi::class)
@Composable
fun UniversalPreviewDialog(
    target: PreviewTarget,
    onDismiss: () -> Unit,
    onDownload: (() -> Unit)? = null,
    onPlayIn8D: (() -> Unit)? = null,
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            shape = RoundedCornerShape(20.dp),
            color = YtCard,
            tonalElevation = 8.dp,
            shadowElevation = 12.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 6.dp, top = 10.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val headerIcon = when (target.kind) {
                        PreviewKind.VIDEO -> Icons.Default.VideoFile
                        PreviewKind.AUDIO -> Icons.Default.AudioFile
                        PreviewKind.IMAGE -> Icons.Default.Image
                        PreviewKind.FILE -> Icons.Default.Description
                    }
                    Icon(headerIcon, contentDescription = null, tint = YtOrange, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = target.title.ifBlank { "Pratinjau ${target.kind.label}" },
                            style = MaterialTheme.typography.titleMedium,
                            color = YtTextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val meta = buildString {
                            append(target.kind.label)
                            if (target.ext.isNotBlank()) append(" · ${target.ext.uppercase()}")
                            if (target.subtitle.isNotBlank()) append(" · ${target.subtitle}")
                        }
                        Text(
                            text = meta,
                            style = MaterialTheme.typography.bodySmall,
                            color = YtTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("preview_close_btn"),
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup pratinjau", tint = YtTextPrimary)
                    }
                }

                HorizontalDivider(color = YtDivider)

                when (target.kind) {
                    PreviewKind.VIDEO, PreviewKind.AUDIO -> {
                        MediaExoPreviewContent(
                            target = target,
                            onPlayIn8D = onPlayIn8D,
                            onDismiss = onDismiss,
                        )
                    }

                    PreviewKind.IMAGE -> {
                        ImagePreviewContent(target = target)
                    }

                    PreviewKind.FILE -> {
                        OtherFilePreviewContent(target = target)
                    }
                }

                HorizontalDivider(color = YtDivider)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            runCatching {
                                val uri = parseTargetUri(target.urlOrUri)
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    if (target.mimeType.isNotBlank()) {
                                        setDataAndType(uri, target.mimeType)
                                    } else {
                                        data = uri
                                    }
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            }.onFailure {
                                AppDiagnostics.warn("Preview", "External open failed: ${it.message}")
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("preview_open_external_btn"),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, YtDivider),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = YtTextPrimary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Buka Eksternal", color = YtTextPrimary, maxLines = 1)
                    }

                    if (onDownload != null) {
                        Button(
                            onClick = {
                                onDownload()
                                onDismiss()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("preview_download_btn"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = YtOrange, contentColor = Color.White),
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Download", fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun MediaExoPreviewContent(
    target: PreviewTarget,
    onPlayIn8D: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var isPlaying by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }

    val resolvedVideoId = remember(target.id, target.urlOrUri, target.videoId) {
        target.videoId?.takeIf { it.isNotBlank() } ?: PlatformDetector.extractYoutubeId(target.urlOrUri)
    }
    var activeStreamUrl by remember(target.id, target.urlOrUri) {
        mutableStateOf(if (MediaScraper.isDirectMediaUrl(target.urlOrUri)) target.urlOrUri else "")
    }
    var activeUserAgent by remember(target.id, target.urlOrUri) {
        mutableStateOf(target.userAgent.ifBlank { "YMusic-Lite/0.3.0" })
    }
    var triedFreshResolve by remember(target.id, target.urlOrUri) { mutableStateOf(false) }

    val httpFactory = remember(target.id, target.urlOrUri) {
        DefaultHttpDataSource.Factory()
            .setUserAgent(activeUserAgent)
            .setAllowCrossProtocolRedirects(true)
    }

    val exoPlayer = remember(target.id, target.urlOrUri) {
        val dataSourceFactory = DefaultDataSource.Factory(context, httpFactory)

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build()
    }

    LaunchedEffect(exoPlayer, activeStreamUrl) {
        val stream = activeStreamUrl.trim()
        if (stream.isBlank()) {
            val vid = resolvedVideoId
            if (!vid.isNullOrBlank() && !triedFreshResolve) {
                triedFreshResolve = true
                isBuffering = true
                errorMsg = null
                val resolved = withContext(Dispatchers.IO) {
                    runCatching { InnerTubeClient.resolveStreams(vid) }.getOrNull()
                }
                val pick = if (target.kind == PreviewKind.VIDEO) {
                    resolved?.videoStreams?.firstOrNull { it.hasAudio } ?: resolved?.bestVideo ?: resolved?.bestAudio
                } else {
                    resolved?.bestAudio ?: resolved?.videoStreams?.firstOrNull { it.hasAudio }
                }
                if (pick != null) {
                    activeUserAgent = pick.userAgent.ifBlank { "YMusic-Lite/0.3.0" }
                    activeStreamUrl = pick.url
                } else {
                    isBuffering = false
                    errorMsg = "Stream media langsung tidak tersedia untuk pratinjau."
                }
            } else {
                // Non-direct HTTP URL without YouTube videoId: probe before preparing
                val raw = target.urlOrUri.trim()
                val playable = withContext(Dispatchers.IO) {
                    InnerTubeClient.probeStreamPlayable(raw, activeUserAgent)
                }
                if (playable) {
                    activeStreamUrl = raw
                } else {
                    isBuffering = false
                    errorMsg = "Tautan halaman web tidak bisa diputar langsung di ExoPlayer. Gunakan Buka Eksternal atau Download."
                }
            }
            return@LaunchedEffect
        }

        httpFactory.setUserAgent(activeUserAgent)
        val uri = parseTargetUri(stream)
        val mediaItemBuilder = MediaItem.Builder().setUri(uri)
        val lower = stream.lowercase()
        when {
            lower.contains(".m3u8") || lower.contains("hls_playlist") ->
                mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_M3U8)
            lower.contains(".mpd") ->
                mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_MPD)
        }
        errorMsg = null
        isBuffering = true
        exoPlayer.setMediaItem(mediaItemBuilder.build())
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                if (exoPlayer.duration > 0) durationMs = exoPlayer.duration
            }

            override fun onPlayerError(error: PlaybackException) {
                AppDiagnostics.warn("Preview", "ExoPlayer preview error for ${target.title}: ${error.errorCodeName}")
                val vid = resolvedVideoId
                if (!vid.isNullOrBlank() && !triedFreshResolve) {
                    triedFreshResolve = true
                    isBuffering = true
                    scope.launch {
                        val resolved = withContext(Dispatchers.IO) {
                            runCatching { InnerTubeClient.resolveStreams(vid) }.getOrNull()
                        }
                        val pool = if (target.kind == PreviewKind.VIDEO) {
                            resolved?.videoStreams.orEmpty() + resolved?.audioStreams.orEmpty()
                        } else {
                            resolved?.audioStreams.orEmpty() + resolved?.videoStreams.orEmpty().filter { it.hasAudio }
                        }
                        val alt = pool.firstOrNull { it.url != activeStreamUrl }
                        if (alt != null) {
                            activeUserAgent = alt.userAgent.ifBlank { "YMusic-Lite/0.3.0" }
                            activeStreamUrl = alt.url
                        } else {
                            isBuffering = false
                            errorMsg = "Tidak dapat memutar pratinjau (${error.errorCodeName})"
                        }
                    }
                } else {
                    isBuffering = false
                    errorMsg = "Tidak dapat memutar pratinjau (${error.errorCodeName})"
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    LaunchedEffect(exoPlayer) {
        while (isActive) {
            positionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
            if (exoPlayer.duration > 0L) durationMs = exoPlayer.duration
            delay(350)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (target.kind == PreviewKind.VIDEO) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center,
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = true
                        }
                    },
                    update = { pv ->
                        if (pv.player !== exoPlayer) pv.player = exoPlayer
                    },
                    onRelease = { pv ->
                        pv.player = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                )
                if (isBuffering && errorMsg == null) {
                    CircularProgressIndicator(color = YtOrange, modifier = Modifier.size(32.dp))
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(YtElevated)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(YtCard),
                    contentAlignment = Alignment.Center,
                ) {
                    if (target.thumbnail.isNotBlank()) {
                        AsyncImage(
                            model = target.thumbnail,
                            contentDescription = target.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(56.dp),
                        )
                    } else {
                        Icon(Icons.Default.AudioFile, contentDescription = null, tint = YtOrange, modifier = Modifier.size(28.dp))
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = target.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = YtTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${formatMs(positionMs)} / ${formatMs(durationMs)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = YtTextSecondary,
                    )
                }
                IconButton(
                    onClick = { if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play() },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(YtOrange)
                        .testTag("preview_audio_toggle_btn"),
                ) {
                    if (isBuffering && !isPlaying && errorMsg == null) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                    } else {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Jeda" else "Putar",
                            tint = Color.White,
                        )
                    }
                }
            }

            Slider(
                value = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f,
                onValueChange = { ratio ->
                    if (durationMs > 0L) exoPlayer.seekTo((ratio * durationMs).toLong())
                },
                colors = SliderDefaults.colors(
                    thumbColor = YtOrange,
                    activeTrackColor = YtOrange,
                    inactiveTrackColor = YtDivider,
                ),
            )
        }

        if (!errorMsg.isNullOrBlank()) {
            Text(
                text = errorMsg!!,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFC62828),
            )
        }

        if (onPlayIn8D != null) {
            OutlinedButton(
                onClick = {
                    exoPlayer.pause()
                    onPlayIn8D()
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("preview_play_8d_btn"),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, YtOrange),
            ) {
                Icon(Icons.Default.Headphones, contentDescription = null, tint = YtOrange, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Putar di Pemutar Utama 8D DSP", color = YtOrange, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ImagePreviewContent(target: PreviewTarget) {
    Column(modifier = Modifier.padding(16.dp)) {
        AsyncImage(
            model = parseTargetUri(target.urlOrUri),
            contentDescription = target.title,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 180.dp, max = 360.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(YtElevated),
        )
    }
}

@Composable
private fun OtherFilePreviewContent(target: PreviewTarget) {
    val context = LocalContext.current
    var fileSizeLabel by remember(target.urlOrUri) { mutableStateOf("Memeriksa berkas…") }
    var textSnippet by remember(target.urlOrUri) { mutableStateOf<String?>(null) }

    LaunchedEffect(target.urlOrUri) {
        withContext(Dispatchers.IO) {
            val uri = parseTargetUri(target.urlOrUri)
            when (uri.scheme) {
                "content" -> {
                    var sizeBytes = -1L
                    runCatching {
                        context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { c ->
                            if (c.moveToFirst()) {
                                val idx = c.getColumnIndex(OpenableColumns.SIZE)
                                if (idx >= 0) sizeBytes = c.getLong(idx)
                            }
                        }
                    }
                    fileSizeLabel = if (sizeBytes > 0) formatBytes(sizeBytes) else "Berkas MediaStore"
                    if (isTextLike(target.mimeType, target.ext)) {
                        runCatching {
                            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                                val buf = CharArray(2048)
                                val n = reader.read(buf)
                                if (n > 0) textSnippet = String(buf, 0, n)
                            }
                        }
                    }
                }
                "file", null -> {
                    val f = File(uri.path ?: target.urlOrUri)
                    fileSizeLabel = if (f.exists()) formatBytes(f.length()) else "Path lokal"
                    if (f.exists() && isTextLike(target.mimeType, target.ext)) {
                        runCatching { textSnippet = f.readText().take(2048) }
                    }
                }
                else -> {
                    fileSizeLabel = "Tautan eksternal (${uri.host ?: "web"})"
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(YtElevated)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("Info Berkas", style = MaterialTheme.typography.labelMedium, color = YtOrange, fontWeight = FontWeight.SemiBold)
            Text("Tipe: ${target.mimeType.ifBlank { target.ext.uppercase().ifBlank { "Umum" } }}", style = MaterialTheme.typography.bodySmall, color = YtTextPrimary)
            Text("Ukuran / Sumber: $fileSizeLabel", style = MaterialTheme.typography.bodySmall, color = YtTextSecondary)
            Text(
                text = target.urlOrUri,
                style = MaterialTheme.typography.labelSmall,
                color = YtTextMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (!textSnippet.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 180.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF14181F))
                    .verticalScroll(rememberScrollState())
                    .padding(10.dp),
            ) {
                Text(
                    text = textSnippet!!,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = Color(0xFFD8E4EE),
                )
            }
        }
    }
}

private fun parseTargetUri(raw: String): Uri {
    val trimmed = raw.trim()
    return if (trimmed.startsWith("/")) Uri.fromFile(File(trimmed)) else Uri.parse(trimmed)
}

private fun isTextLike(mime: String, ext: String): Boolean {
    val m = mime.lowercase()
    val e = ext.lowercase().trimStart('.')
    return m.startsWith("text/") || m.contains("json") || m.contains("xml") ||
        e in setOf("txt", "json", "xml", "csv", "log", "md", "m3u8", "srt", "vtt")
}

private fun formatMs(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val totalSec = ms / 1000L
    val m = totalSec / 60L
    val s = totalSec % 60L
    return "$m:${s.toString().padStart(2, '0')}"
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024L) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024.0) return String.format("%.1f KB", kb)
    val mb = kb / 1024.0
    return String.format("%.2f MB", mb)
}
