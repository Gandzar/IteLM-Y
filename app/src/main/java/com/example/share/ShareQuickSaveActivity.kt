package com.example.share

import android.Manifest
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.YMusicApplication
import com.example.data.AppSettings
import com.example.data.DownloadFormat
import com.example.data.MediaItem
import com.example.data.PlatformId
import com.example.data.PreviewKind
import com.example.data.PreviewTarget
import com.example.data.ResolvedStreams
import com.example.data.ScrapeResult
import com.example.data.StreamCandidate
import com.example.data.Track
import com.example.data.TrackSource
import com.example.debug.AppDiagnostics
import com.example.engine.MediaScraper
import com.example.engine.PlatformDetector
import com.example.engine.ServerManager
import com.example.notify.AppNotifications
import com.example.player.BackgroundPlaybackService
import com.example.ui.components.QualityPickerDialog
import com.example.ui.components.UniversalPreviewDialog
import com.example.ui.theme.DangerRed
import com.example.ui.theme.SpatialGreen
import com.example.ui.theme.YMusicLiteTheme
import com.example.ui.theme.YtCard
import com.example.ui.theme.YtDivider
import com.example.ui.theme.YtElevated
import com.example.ui.theme.YtOrange
import com.example.ui.theme.YtOrangeSoft
import com.example.ui.theme.YtTextMuted
import com.example.ui.theme.YtTextPrimary
import com.example.ui.theme.YtTextSecondary
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ShareQuickSaveActivity : ComponentActivity() {
    private var sharedPayload by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        sharedPayload = extractSharedText(intent)
        if (sharedPayload.isBlank()) {
            finish()
            return
        }
        AppDiagnostics.info("QuickSave", "Opened overlay for shared URL: $sharedPayload")

        setContent {
            YMusicLiteTheme {
                QuickSaveOverlay(
                    rawPayload = sharedPayload,
                    onDismiss = { finish() },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val extracted = extractSharedText(intent)
        if (extracted.isNotBlank()) {
            sharedPayload = extracted
        }
    }

    private fun extractSharedText(intent: Intent?): String {
        if (intent == null) return ""
        val raw = intent.getStringExtra(Intent.EXTRA_TEXT)
            ?: intent.getStringExtra(Intent.EXTRA_SUBJECT)
            ?: return ""
        return Regex("https?://\\S+").find(raw)?.value ?: raw.trim()
    }
}

@Composable
private fun QuickSaveOverlay(
    rawPayload: String,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val app = remember { YMusicApplication.instance }
    val scope = rememberCoroutineScope()
    val settings by app.repository.settingsFlow.collectAsStateWithLifecycle(initialValue = AppSettings())

    var activeServerId by remember { mutableStateOf(settings.activeServerId) }
    LaunchedEffect(settings.activeServerId) {
        activeServerId = settings.activeServerId
    }
    val allServers = remember(settings) { ServerManager.allServers(settings) }
    val activeServer = remember(allServers, activeServerId) {
        allServers.firstOrNull { it.id == activeServerId } ?: allServers.first()
    }

    var loading by remember(rawPayload) { mutableStateOf(true) }
    var scrapeResult by remember(rawPayload) { mutableStateOf<ScrapeResult?>(null) }
    var resolvedStreams by remember(rawPayload) { mutableStateOf<ResolvedStreams?>(null) }
    var errorMsg by remember(rawPayload) { mutableStateOf<String?>(null) }
    var savedToLibrary by remember(rawPayload) { mutableStateOf(false) }
    var previewTarget by remember { mutableStateOf<PreviewTarget?>(null) }
    var pickerInitialFormat by remember { mutableStateOf<DownloadFormat?>(null) }
    var showQualityPicker by remember { mutableStateOf(false) }
    var statusFeedback by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableStateOf(0) }

    val notifPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { /* Notification appears automatically if granted; download runs either way */ }

    fun requestNotifPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !AppNotifications.hasNotificationPermission(context)
        ) {
            runCatching { notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
        }
    }

    LaunchedEffect(rawPayload, activeServerId, reloadKey) {
        loading = true
        errorMsg = null
        val headers = app.authManager.buildAuthHeaders()
        val srv = ServerManager.resolveActiveServer(settings, activeServerId)
        val scrape = runCatching { MediaScraper.scrapeUrl(rawPayload, headers, srv) }.getOrNull()
        scrapeResult = scrape
        val ytId = PlatformDetector.extractYoutubeId(rawPayload)
        if (!ytId.isNullOrBlank()) {
            resolvedStreams = runCatching { ServerManager.resolveWithServer(ytId, srv, headers) }.getOrNull()
        } else {
            resolvedStreams = null
        }

        if (scrape == null && resolvedStreams == null) {
            errorMsg = "Gagal mengekstrak link media dari ${srv.name}"
            AppDiagnostics.warn("QuickSave", "Scrape returned null for $rawPayload (${srv.name})")
        } else {
            val currentLib = app.repository.libraryFlow.first()
            val probeTrack = scrape?.let { MediaScraper.scrapeToTrack(it) }
            savedToLibrary = probeTrack != null && currentLib.any { it.id == probeTrack.id }
            AppDiagnostics.info(
                "QuickSave",
                "Scraped '${scrape?.title ?: resolvedStreams?.title}' via ${srv.name}",
            )
        }
        loading = false
    }

    fun buildTrack(): Track {
        val scrape = scrapeResult
        val fromScrape = scrape?.let { MediaScraper.scrapeToTrack(it) }
        if (fromScrape != null) {
            return fromScrape.copy(
                url = resolvedStreams?.bestAudio?.url ?: fromScrape.url,
            )
        }
        val ytId = PlatformDetector.extractYoutubeId(rawPayload)
        return Track(
            id = ytId ?: "qs-${rawPayload.hashCode()}",
            title = resolvedStreams?.title?.ifBlank { null } ?: "Media Dibagikan",
            artist = resolvedStreams?.author?.ifBlank { null } ?: PlatformDetector.platformLabel(PlatformDetector.detectPlatform(rawPayload)),
            duration = resolvedStreams?.durationSec ?: 0,
            thumbnail = resolvedStreams?.thumbnail.orEmpty(),
            source = if (ytId != null) TrackSource.YOUTUBE else TrackSource.URL,
            videoId = ytId,
            url = resolvedStreams?.bestAudio?.url ?: rawPayload,
        )
    }

    fun startQuickDownload(
        format: DownloadFormat,
        overrideUrl: String? = null,
        overrideExt: String? = null,
        overrideUa: String? = null,
    ) {
        requestNotifPermissionIfNeeded()
        val base = buildTrack()
        val track = if (!overrideUrl.isNullOrBlank()) base.copy(url = overrideUrl) else base
        app.downloadManager.enqueue(
            track = track,
            format = format,
            overrideExt = overrideExt,
            overrideUserAgent = overrideUa,
        )
        app.postGlobalToast("Download ${format.label} dimulai di background")
        onDismiss()
    }

    fun switchServer(newServerId: String) {
        activeServerId = newServerId
        scope.launch {
            app.repository.updateSettings { it.copy(activeServerId = newServerId) }
        }
    }

    val titleText = scrapeResult?.title?.ifBlank { null }
        ?: resolvedStreams?.title?.ifBlank { null }
        ?: rawPayload
    val artistText = scrapeResult?.author?.ifBlank { null }
        ?: resolvedStreams?.author?.ifBlank { null }
        ?: rawPayload
    val thumbUrl = scrapeResult?.thumbnail ?: resolvedStreams?.thumbnail

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.56f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            )
            .systemBarsPadding()
            .padding(horizontal = 16.dp, vertical = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = YtCard),
            border = BorderStroke(1.dp, YtDivider),
            elevation = CardDefaults.cardElevation(defaultElevation = 14.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 660.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        color = YtOrangeSoft,
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            text = "QUICK SAVE",
                            style = MaterialTheme.typography.labelSmall,
                            color = YtOrange,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = scrapeResult?.platformLabel ?: PlatformDetector.platformLabel(PlatformDetector.detectPlatform(rawPayload)),
                        style = MaterialTheme.typography.labelMedium,
                        color = YtTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = { ShareHelper.shareTrackLink(context, buildTrack()) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("quicksave_share_btn"),
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Bagikan tautan", tint = YtTextPrimary)
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("quicksave_close_btn"),
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup dialog", tint = YtTextPrimary)
                    }
                }

                // Media preview summary
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(YtElevated)
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(YtCard),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (!thumbUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = thumbUrl,
                                contentDescription = titleText,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Icon(Icons.Default.AudioFile, contentDescription = null, tint = YtOrange, modifier = Modifier.size(28.dp))
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = titleText,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = YtTextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = artistText,
                            style = MaterialTheme.typography.bodySmall,
                            color = YtTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                // Active Server quick selector
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Dns, contentDescription = null, tint = YtOrange, modifier = Modifier.size(16.dp))
                        Text(
                            text = "Server Scraper: ${activeServer.name}",
                            style = MaterialTheme.typography.labelMedium,
                            color = YtTextSecondary,
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        allServers.forEach { srv ->
                            val selected = srv.id == activeServer.id
                            FilterChip(
                                selected = selected,
                                onClick = { switchServer(srv.id) },
                                label = { Text(srv.name, maxLines = 1) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = YtOrangeSoft,
                                    selectedLabelColor = YtOrange,
                                    labelColor = YtTextSecondary,
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selected,
                                    borderColor = YtDivider,
                                    selectedBorderColor = YtOrange,
                                ),
                                modifier = Modifier.testTag("quicksave_server_${srv.id}"),
                            )
                        }
                    }
                }

                if (loading) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(color = YtOrange, strokeWidth = 2.5.dp, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Mengekstrak resolusi & ukuran (MB)…", style = MaterialTheme.typography.bodySmall, color = YtTextSecondary)
                    }
                } else if (!errorMsg.isNullOrBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(errorMsg!!, style = MaterialTheme.typography.bodySmall, color = DangerRed, modifier = Modifier.weight(1f))
                        OutlinedButton(onClick = { reloadKey++ }, shape = RoundedCornerShape(10.dp)) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Ulangi")
                        }
                    }
                }

                // Quick actions: Background 8D + Save to Local Library
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = {
                            requestNotifPermissionIfNeeded()
                            val track = buildTrack()
                            app.player.play(track)
                            BackgroundPlaybackService.sync(context)
                            app.postGlobalToast("Memutar 8D di latar belakang: ${track.title}")
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quicksave_play_8d_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF153629), contentColor = SpatialGreen),
                    ) {
                        Icon(Icons.Default.Headphones, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Putar 8D", fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = {
                            val track = buildTrack()
                            scope.launch {
                                val current = app.repository.libraryFlow.first()
                                if (!current.any { it.id == track.id }) {
                                    app.repository.saveLibrary(listOf(track) + current)
                                }
                                savedToLibrary = true
                                statusFeedback = "Tersimpan ke Library lokal"
                                app.postGlobalToast("Disimpan ke Library: ${track.title}")
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quicksave_save_library_btn"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (savedToLibrary) YtOrange else YtDivider),
                    ) {
                        Icon(
                            imageVector = if (savedToLibrary) Icons.Default.BookmarkAdded else Icons.Default.LibraryAdd,
                            contentDescription = null,
                            tint = if (savedToLibrary) YtOrange else YtTextPrimary,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (savedToLibrary) "Tersimpan" else "Library",
                            color = if (savedToLibrary) YtOrange else YtTextPrimary,
                            maxLines = 1,
                        )
                    }
                }

                // Download MP3 / MP4 -> opens Quality & MB Picker
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = {
                            pickerInitialFormat = DownloadFormat.MP3
                            showQualityPicker = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("quicksave_dl_mp3_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = YtOrange, contentColor = Color.White),
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Download MP3", fontWeight = FontWeight.Bold, maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = {
                            pickerInitialFormat = DownloadFormat.MP4
                            showQualityPicker = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("quicksave_dl_mp4_btn"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.2.dp, YtOrange),
                    ) {
                        Icon(Icons.Default.VideoFile, contentDescription = null, tint = YtOrange, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Download MP4", color = YtOrange, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }

                if (!statusFeedback.isNullOrBlank()) {
                    Text(
                        text = statusFeedback!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = YtOrange,
                        fontWeight = FontWeight.Medium,
                    )
                }

                // Detailed scraped streams & resolutions list with MB Size & Preview buttons
                val candidates: List<StreamCandidate> = remember(resolvedStreams) {
                    buildList {
                        resolvedStreams?.audioStreams?.forEach { add(it) }
                        resolvedStreams?.videoStreams?.forEach { add(it) }
                    }
                }
                val fallbackItems: List<MediaItem> = scrapeResult?.items.orEmpty()

                if (candidates.isNotEmpty() || fallbackItems.isNotEmpty()) {
                    HorizontalDivider(color = YtDivider)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "Semua Format & MB (${if (candidates.isNotEmpty()) candidates.size else fallbackItems.size})",
                            style = MaterialTheme.typography.labelLarge,
                            color = YtTextPrimary,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    pickerInitialFormat = null
                                    showQualityPicker = true
                                }
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = YtOrange, modifier = Modifier.size(15.dp))
                            Text("Filter", style = MaterialTheme.typography.labelMedium, color = YtOrange)
                        }
                    }
                }

                if (candidates.isNotEmpty()) {
                    candidates.forEachIndexed { idx, stream ->
                        val fmt = if (stream.isAudio) DownloadFormat.MP3 else DownloadFormat.MP4
                        val sizePart = stream.formattedSize.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()
                        val typePart = if (stream.isAudio) "Audio" else if (stream.hasAudio) "Video+Audio" else "Video Only"
                        QuickSaveStreamRow(
                            title = "${stream.qualityLabel.ifBlank { stream.ext }}$sizePart",
                            subtitle = "${stream.ext.uppercase()} · $typePart${if (stream.bitrate > 0) " · ${stream.bitrate / 1000} kbps" else ""}",
                            isAudio = stream.isAudio,
                            testTagPrefix = "quicksave_stream_$idx",
                            onPreview = {
                                previewTarget = PreviewTarget(
                                    id = "qs-preview-$idx",
                                    title = titleText,
                                    subtitle = "${stream.qualityLabel}$sizePart",
                                    urlOrUri = stream.url,
                                    mimeType = stream.mimeType,
                                    ext = stream.ext,
                                    kind = if (stream.isAudio) PreviewKind.AUDIO else PreviewKind.VIDEO,
                                    thumbnail = thumbUrl.orEmpty(),
                                    videoId = resolvedStreams?.videoId,
                                    userAgent = stream.userAgent.ifBlank { "YMusic-Lite/0.3.0" },
                                )
                            },
                            onDownload = {
                                startQuickDownload(
                                    format = fmt,
                                    overrideUrl = stream.url,
                                    overrideExt = stream.ext,
                                    overrideUa = stream.userAgent,
                                )
                            },
                        )
                    }
                } else {
                    fallbackItems.forEachIndexed { idx, item ->
                        val isAudio = item.type == "audio" || item.ext?.lowercase() in setOf("mp3", "m4a", "opus", "wav", "ogg")
                        val fmt = if (isAudio) DownloadFormat.MP3 else DownloadFormat.MP4
                        val ext = item.ext ?: if (isAudio) "m4a" else "mp4"
                        val sizePart = item.size?.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()
                        QuickSaveStreamRow(
                            title = "${item.type.uppercase()} · ${item.quality ?: ext.uppercase()}$sizePart",
                            subtitle = ext.uppercase(),
                            isAudio = isAudio,
                            testTagPrefix = "quicksave_item_$idx",
                            onPreview = {
                                previewTarget = PreviewTarget(
                                    id = "qs-item-$idx",
                                    title = titleText,
                                    subtitle = "${item.quality.orEmpty()}$sizePart",
                                    urlOrUri = item.url,
                                    ext = ext,
                                    kind = PreviewTarget.detectKind(null, ext, item.type),
                                    thumbnail = thumbUrl.orEmpty(),
                                    videoId = PlatformDetector.extractYoutubeId(rawPayload),
                                )
                            },
                            onDownload = {
                                startQuickDownload(
                                    format = fmt,
                                    overrideUrl = item.url,
                                    overrideExt = item.ext,
                                )
                            },
                        )
                    }
                }
            }
        }
    }

    if (showQualityPicker) {
        QualityPickerDialog(
            title = titleText,
            subtitle = artistText,
            thumbnail = thumbUrl.orEmpty(),
            allServers = allServers,
            activeServer = activeServer,
            resolvedStreams = resolvedStreams,
            scrapeResult = scrapeResult,
            isLoading = loading,
            errorMessage = errorMsg,
            initialFormat = pickerInitialFormat,
            onSelectServer = { switchServer(it) },
            onOpenServerMenu = null,
            onRetry = { reloadKey++ },
            onDownloadCandidate = { c ->
                showQualityPicker = false
                startQuickDownload(
                    format = if (c.isAudio) DownloadFormat.MP3 else DownloadFormat.MP4,
                    overrideUrl = c.url,
                    overrideExt = c.ext,
                    overrideUa = c.userAgent,
                )
            },
            onPreviewCandidate = { c ->
                previewTarget = PreviewTarget(
                    id = "qs-qp-${c.url.hashCode()}",
                    title = titleText,
                    subtitle = "${c.qualityLabel} · ${c.formattedSize}",
                    urlOrUri = c.url,
                    mimeType = c.mimeType,
                    ext = c.ext,
                    kind = if (c.isAudio) PreviewKind.AUDIO else PreviewKind.VIDEO,
                    thumbnail = thumbUrl.orEmpty(),
                    videoId = resolvedStreams?.videoId,
                    userAgent = c.userAgent.ifBlank { "YMusic-Lite/0.3.0" },
                )
            },
            onDownloadMediaItem = { item ->
                showQualityPicker = false
                val isAudio = item.type == "audio" || item.ext?.lowercase() in setOf("mp3", "m4a", "opus", "wav", "ogg")
                startQuickDownload(
                    format = if (isAudio) DownloadFormat.MP3 else DownloadFormat.MP4,
                    overrideUrl = item.url,
                    overrideExt = item.ext,
                )
            },
            onPreviewMediaItem = { item ->
                val ext = item.ext.orEmpty()
                previewTarget = PreviewTarget(
                    id = "qs-qp-item-${item.url.hashCode()}",
                    title = titleText,
                    subtitle = "${item.quality.orEmpty()} ${item.size.orEmpty()}",
                    urlOrUri = item.url,
                    ext = ext,
                    kind = PreviewTarget.detectKind(null, ext, item.type),
                    thumbnail = thumbUrl.orEmpty(),
                    videoId = PlatformDetector.extractYoutubeId(rawPayload),
                )
            },
            onShare = { ShareHelper.shareTrackLink(context, buildTrack()) },
            onDismiss = { showQualityPicker = false },
        )
    }

    previewTarget?.let { target ->
        UniversalPreviewDialog(
            target = target,
            onDismiss = { previewTarget = null },
            onDownload = {
                val fmt = if (target.kind == PreviewKind.AUDIO) DownloadFormat.MP3 else DownloadFormat.MP4
                startQuickDownload(
                    format = fmt,
                    overrideUrl = target.urlOrUri,
                    overrideExt = target.ext.ifBlank { null },
                    overrideUa = target.userAgent,
                )
            },
            onPlayIn8D = {
                val track = buildTrack()
                app.player.play(track)
                BackgroundPlaybackService.sync(context)
                app.postGlobalToast("Memutar 8D di latar belakang: ${track.title}")
                onDismiss()
            },
        )
    }
}

@Composable
private fun QuickSaveStreamRow(
    title: String,
    subtitle: String,
    isAudio: Boolean,
    testTagPrefix: String,
    onPreview: () -> Unit,
    onDownload: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(YtElevated)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = if (isAudio) Icons.Default.AudioFile else Icons.Default.VideoFile,
            contentDescription = null,
            tint = YtOrange,
            modifier = Modifier.size(20.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = YtTextPrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = YtTextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(
            onClick = onPreview,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("${testTagPrefix}_preview"),
        ) {
            Icon(Icons.Default.PlayCircleOutline, contentDescription = "Pratinjau format", tint = YtTextPrimary)
        }
        IconButton(
            onClick = onDownload,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("${testTagPrefix}_download"),
        ) {
            Icon(Icons.Default.Download, contentDescription = "Unduh format", tint = YtOrange)
        }
    }
}
