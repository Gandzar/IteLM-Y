package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Download
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.DownloadFormat
import com.example.data.MediaItem
import com.example.data.ResolvedStreams
import com.example.data.ScrapeResult
import com.example.data.ScraperServer
import com.example.data.StreamCandidate
import com.example.ui.theme.DangerRed
import com.example.ui.theme.SpatialGreen
import com.example.ui.theme.YtCard
import com.example.ui.theme.YtDivider
import com.example.ui.theme.YtElevated
import com.example.ui.theme.YtOrange
import com.example.ui.theme.YtOrangeSoft
import com.example.ui.theme.YtTextMuted
import com.example.ui.theme.YtTextPrimary
import com.example.ui.theme.YtTextSecondary

enum class QualityFilter(val label: String) {
    ALL("Semua"),
    AUDIO("Audio (MP3/M4A)"),
    VIDEO("Video (MP4/WebM)"),
}

@Composable
fun QualityPickerDialog(
    title: String,
    subtitle: String,
    thumbnail: String,
    allServers: List<ScraperServer>,
    activeServer: ScraperServer,
    resolvedStreams: ResolvedStreams?,
    scrapeResult: ScrapeResult?,
    isLoading: Boolean,
    errorMessage: String?,
    initialFormat: DownloadFormat?,
    onSelectServer: (String) -> Unit,
    onOpenServerMenu: (() -> Unit)?,
    onRetry: () -> Unit,
    onDownloadCandidate: (StreamCandidate) -> Unit,
    onPreviewCandidate: (StreamCandidate) -> Unit,
    onDownloadMediaItem: (MediaItem) -> Unit,
    onPreviewMediaItem: (MediaItem) -> Unit,
    onShare: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    var filter by remember(initialFormat) {
        mutableStateOf(
            when (initialFormat) {
                DownloadFormat.MP3 -> QualityFilter.AUDIO
                DownloadFormat.MP4 -> QualityFilter.VIDEO
                null -> QualityFilter.ALL
            },
        )
    }

    val filteredCandidates: List<StreamCandidate> = remember(resolvedStreams, filter) {
        val list = buildList {
            if (filter == QualityFilter.ALL || filter == QualityFilter.AUDIO) {
                addAll(resolvedStreams?.audioStreams.orEmpty())
            }
            if (filter == QualityFilter.ALL || filter == QualityFilter.VIDEO) {
                addAll(resolvedStreams?.videoStreams.orEmpty())
            }
        }
        list
    }

    val filteredItems: List<MediaItem> = remember(scrapeResult, filter) {
        val raw = scrapeResult?.items.orEmpty()
        when (filter) {
            QualityFilter.ALL -> raw
            QualityFilter.AUDIO -> raw.filter {
                it.type == "audio" || it.ext?.lowercase() in setOf("mp3", "m4a", "opus", "ogg", "wav")
            }
            QualityFilter.VIDEO -> raw.filter {
                it.type != "audio" && it.ext?.lowercase() !in setOf("mp3", "m4a", "opus", "ogg", "wav")
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 22.dp),
            contentAlignment = Alignment.Center,
        ) {
            Card(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .heightIn(max = 680.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = YtCard),
                border = BorderStroke(1.dp, YtDivider),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // Top bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = YtOrange,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Pilih Resolusi & Ukuran",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = YtTextPrimary,
                            modifier = Modifier.weight(1f),
                        )
                        if (onShare != null) {
                            IconButton(
                                onClick = onShare,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("qp_share_btn"),
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Bagikan tautan", tint = YtTextSecondary)
                            }
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("qp_close_btn"),
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup", tint = YtTextPrimary)
                        }
                    }

                    // Track header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(YtElevated)
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(YtCard),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (thumbnail.isNotBlank()) {
                                AsyncImage(
                                    model = thumbnail,
                                    contentDescription = title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else {
                                Icon(Icons.Default.AudioFile, contentDescription = null, tint = YtOrange)
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title.ifBlank { "Media Unduhan" },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = YtTextPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (subtitle.isNotBlank()) {
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = YtTextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }

                    // Active server switcher strip
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(Icons.Default.Dns, contentDescription = null, tint = YtOrange, modifier = Modifier.size(16.dp))
                                Text(
                                    text = "Server: ${resolvedStreams?.sourceServerName?.ifBlank { null } ?: activeServer.name}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = YtTextSecondary,
                                )
                            }
                            if (onOpenServerMenu != null) {
                                Text(
                                    text = "Menu Server",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = YtOrange,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable {
                                            onDismiss()
                                            onOpenServerMenu()
                                        }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                        .testTag("qp_open_server_menu"),
                                )
                            }
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
                                    onClick = { onSelectServer(srv.id) },
                                    label = {
                                        val ping = srv.lastPingMs?.let { " (${it}ms)" }.orEmpty()
                                        Text("${srv.name}$ping", maxLines = 1)
                                    },
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
                                    modifier = Modifier.testTag("qp_server_${srv.id}"),
                                )
                            }
                        }
                    }

                    // Format filter chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        QualityFilter.entries.forEach { tab ->
                            val selected = tab == filter
                            FilterChip(
                                selected = selected,
                                onClick = { filter = tab },
                                label = { Text(tab.label, maxLines = 1) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = YtOrange,
                                    selectedLabelColor = Color.White,
                                    labelColor = YtTextPrimary,
                                ),
                                modifier = Modifier.testTag("qp_filter_${tab.name.lowercase()}"),
                            )
                        }
                    }

                    HorizontalDivider(color = YtDivider)

                    when {
                        isLoading -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                CircularProgressIndicator(color = YtOrange)
                                Text(
                                    text = "Mengambil daftar resolusi & ukuran MB dari ${activeServer.name}…",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = YtTextSecondary,
                                )
                            }
                        }

                        !errorMessage.isNullOrBlank() && filteredCandidates.isEmpty() && filteredItems.isEmpty() -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Text(
                                    text = errorMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DangerRed,
                                )
                                Button(
                                    onClick = onRetry,
                                    colors = ButtonDefaults.buttonColors(containerColor = YtOrange, contentColor = Color.White),
                                    shape = RoundedCornerShape(10.dp),
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Coba Ulang")
                                }
                            }
                        }

                        filteredCandidates.isNotEmpty() -> {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f, fill = false)
                                    .heightIn(max = 360.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                itemsIndexed(filteredCandidates) { idx, candidate ->
                                    CandidateQualityRow(
                                        candidate = candidate,
                                        testTagPrefix = "qp_cand_$idx",
                                        onPreview = { onPreviewCandidate(candidate) },
                                        onDownload = { onDownloadCandidate(candidate) },
                                    )
                                }
                            }
                        }

                        filteredItems.isNotEmpty() -> {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f, fill = false)
                                    .heightIn(max = 360.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                itemsIndexed(filteredItems) { idx, item ->
                                    MediaItemQualityRow(
                                        item = item,
                                        testTagPrefix = "qp_item_$idx",
                                        onPreview = { onPreviewMediaItem(item) },
                                        onDownload = { onDownloadMediaItem(item) },
                                    )
                                }
                            }
                        }

                        else -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = "Tidak ada format pada filter ini. Coba ganti server di atas atau pilih 'Semua'.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = YtTextSecondary,
                                )
                                OutlinedButton(
                                    onClick = onRetry,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, YtDivider),
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, tint = YtOrange, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Muat Ulang", color = YtTextPrimary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CandidateQualityRow(
    candidate: StreamCandidate,
    testTagPrefix: String,
    onPreview: () -> Unit,
    onDownload: () -> Unit,
) {
    val typeTag = when {
        candidate.isAudio -> "Audio Only"
        candidate.hasAudio -> "Video+Audio"
        else -> "Video Only"
    }
    val fpsPart = if (!candidate.isAudio && candidate.fps > 0) " · ${candidate.fps}fps" else ""
    val bitratePart = if (candidate.bitrate > 0) " · ${candidate.bitrate / 1000} kbps" else ""
    val sizeBadge = candidate.formattedSize.ifBlank { "Ukuran dinamis" }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(YtElevated)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = if (candidate.isAudio) Icons.Default.AudioFile else Icons.Default.VideoFile,
            contentDescription = null,
            tint = if (candidate.isAudio) SpatialGreen else YtOrange,
            modifier = Modifier.size(22.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = candidate.qualityLabel.ifBlank { candidate.ext.uppercase() },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = YtTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Surface(
                    color = YtOrangeSoft,
                    shape = RoundedCornerShape(6.dp),
                ) {
                    Text(
                        text = sizeBadge,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = YtOrange,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${candidate.ext.uppercase()} · $typeTag$fpsPart$bitratePart",
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
            Icon(Icons.Default.PlayCircleOutline, contentDescription = "Pratinjau", tint = YtTextPrimary)
        }
        Button(
            onClick = onDownload,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = YtOrange, contentColor = Color.White),
            modifier = Modifier.testTag("${testTagPrefix}_download"),
        ) {
            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Unduh", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MediaItemQualityRow(
    item: MediaItem,
    testTagPrefix: String,
    onPreview: () -> Unit,
    onDownload: () -> Unit,
) {
    val isAudio = item.type == "audio" || item.ext?.lowercase() in setOf("mp3", "m4a", "opus", "ogg", "wav")
    val extLabel = (item.ext ?: if (isAudio) "mp3" else "mp4").uppercase()
    val sizeLabel = item.size?.takeIf { it.isNotBlank() } ?: "Langsung"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(YtElevated)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = if (isAudio) Icons.Default.AudioFile else Icons.Default.VideoFile,
            contentDescription = null,
            tint = if (isAudio) SpatialGreen else YtOrange,
            modifier = Modifier.size(22.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = item.quality?.ifBlank { null } ?: item.type.uppercase(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = YtTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Surface(color = YtOrangeSoft, shape = RoundedCornerShape(6.dp)) {
                    Text(
                        text = sizeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = YtOrange,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            Text(
                text = "${item.type.uppercase()} · $extLabel",
                style = MaterialTheme.typography.labelSmall,
                color = YtTextMuted,
            )
        }

        IconButton(
            onClick = onPreview,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("${testTagPrefix}_preview"),
        ) {
            Icon(Icons.Default.PlayCircleOutline, contentDescription = "Pratinjau", tint = YtTextPrimary)
        }
        Button(
            onClick = onDownload,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = YtOrange, contentColor = Color.White),
            modifier = Modifier.testTag("${testTagPrefix}_download"),
        ) {
            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Unduh", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
    }
}
