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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.SpatialAudioOff
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.data.CommentsPageResult
import com.example.data.LyricsState
import com.example.data.RepeatMode
import com.example.data.Track
import com.example.player.PlayerMode
import com.example.player.PlayerState
import com.example.ui.theme.SpatialGreen
import com.example.ui.theme.YtBg
import com.example.ui.theme.YtCard
import com.example.ui.theme.YtDivider
import com.example.ui.theme.YtElevated
import com.example.ui.theme.YtOrange
import com.example.ui.theme.YtOrangeSoft
import com.example.ui.theme.YtTextMuted
import com.example.ui.theme.YtTextPrimary
import com.example.ui.theme.YtTextSecondary
import java.util.Locale

private enum class NowPlayingTab(val label: String) {
    QUEUE("Antrean"),
    LYRICS("Lirik"),
    SOCIAL("Komentar & Like"),
}

@Composable
fun NowPlayingSheet(
    playerState: PlayerState,
    inLibrary: Boolean,
    isLoggedIn: Boolean,
    commentsState: CommentsPageResult,
    commentsLoading: Boolean,
    commentPosting: Boolean,
    lyricsState: LyricsState,
    onDismiss: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Float) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onSetSpeedAndPitch: (Float, Float) -> Unit,
    onSetSleepMinutes: (Int) -> Unit,
    onSetSleepAfterTrack: () -> Unit,
    onCancelSleepTimer: () -> Unit,
    onToggleLibrary: (Track) -> Unit,
    onSwitchAudioVideo: () -> Unit,
    onOpen8DStudio: () -> Unit,
    onDownloadQualityPicker: (Track) -> Unit,
    onShareTrack: (Track) -> Unit,
    onSelectQueueIndex: (Int) -> Unit,
    onRemoveQueueIndex: (Int) -> Unit,
    onLoadComments: (String) -> Unit,
    onLoadMoreComments: () -> Unit,
    onToggleLikeYoutube: () -> Unit,
    onSubmitComment: (String) -> Unit,
    onLoadLyrics: (String) -> Unit,
    onOpenLogin: () -> Unit,
) {
    val track = playerState.track ?: return
    var activeTab by remember { mutableStateOf(NowPlayingTab.QUEUE) }
    var showSleepDialog by remember { mutableStateOf(false) }
    var showSpeedPanel by remember { mutableStateOf(false) }
    var commentDraft by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(YtBg)
                .systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Top Header
            Row(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("np_collapse_btn"),
                ) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Tutup layar penuh", tint = YtTextPrimary)
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "SEDANG DIPUTAR",
                        style = MaterialTheme.typography.labelSmall,
                        color = YtTextMuted,
                        fontWeight = FontWeight.Bold,
                    )
                    if (playerState.audioCodecLabel.isNotBlank()) {
                        Text(
                            text = playerState.audioCodecLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = YtOrange,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                IconButton(
                    onClick = { onShareTrack(track) },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("np_share_btn"),
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Bagikan lagu", tint = YtTextPrimary)
                }
                IconButton(
                    onClick = { onDownloadQualityPicker(track) },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("np_download_btn"),
                ) {
                    Icon(Icons.Default.Download, contentDescription = "Unduh lagu", tint = YtOrange)
                }
            }

            Column(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Artwork Card
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(YtElevated),
                    contentAlignment = Alignment.Center,
                ) {
                    if (track.thumbnail.isNotBlank()) {
                        AsyncImage(
                            model = track.thumbnail,
                            contentDescription = track.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = YtOrange, modifier = Modifier.size(64.dp))
                    }
                }

                // Title + Artist + Library Heart
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = YtTextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = track.artist,
                            style = MaterialTheme.typography.bodyMedium,
                            color = YtTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(
                        onClick = { onToggleLibrary(track) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("np_library_btn"),
                    ) {
                        Icon(
                            imageVector = if (inLibrary) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Simpan ke Library",
                            tint = if (inLibrary) YtOrange else YtTextSecondary,
                        )
                    }
                }

                // Seekbar
                val duration = playerState.durationSec.coerceAtLeast(1f)
                val pos = playerState.currentTimeSec.coerceIn(0f, duration)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Slider(
                        value = pos,
                        onValueChange = { onSeek(it) },
                        valueRange = 0f..duration,
                        colors = SliderDefaults.colors(
                            thumbColor = YtOrange,
                            activeTrackColor = YtOrange,
                            inactiveTrackColor = YtDivider,
                        ),
                        modifier = Modifier.testTag("np_seek_slider"),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(formatSecTime(pos.toInt()), style = MaterialTheme.typography.labelSmall, color = YtTextMuted)
                        Text(formatSecTime(duration.toInt()), style = MaterialTheme.typography.labelSmall, color = YtTextMuted)
                    }
                }

                // Transport Controls (Shuffle / Prev / Play-Pause / Next / Repeat)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = onToggleShuffle,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("np_shuffle_btn"),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Acak",
                            tint = if (playerState.shuffleEnabled) YtOrange else YtTextSecondary,
                        )
                    }
                    IconButton(
                        onClick = onPrevious,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("np_prev_btn"),
                    ) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Sebelumnya", tint = YtTextPrimary, modifier = Modifier.size(34.dp))
                    }
                    Surface(
                        color = YtOrange,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(64.dp)
                            .clickable(onClick = onTogglePlayPause)
                            .testTag("np_play_pause_btn"),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (playerState.playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (playerState.playing) "Jeda" else "Putar",
                                tint = Color.White,
                                modifier = Modifier.size(34.dp),
                            )
                        }
                    }
                    IconButton(
                        onClick = onNext,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("np_next_btn"),
                    ) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Berikutnya", tint = YtTextPrimary, modifier = Modifier.size(34.dp))
                    }
                    IconButton(
                        onClick = onCycleRepeat,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("np_repeat_btn"),
                    ) {
                        Icon(
                            imageVector = if (playerState.repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                            contentDescription = playerState.repeatMode.label,
                            tint = if (playerState.repeatMode == RepeatMode.OFF) YtTextSecondary else YtOrange,
                        )
                    }
                }

                // YMusic / Mi Music Utility Chips Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val sleepLabel = when {
                        playerState.sleepAfterCurrentTrack -> "Timer: Akhir Lagu"
                        playerState.sleepTimerRemainingSec != null -> "Timer: ${formatSecTime(playerState.sleepTimerRemainingSec.toInt())}"
                        else -> "Sleep Timer"
                    }
                    FilterChip(
                        selected = playerState.sleepTimerRemainingSec != null || playerState.sleepAfterCurrentTrack,
                        onClick = { showSleepDialog = !showSleepDialog },
                        leadingIcon = { Icon(Icons.Default.Bedtime, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text(sleepLabel, maxLines = 1) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = YtOrangeSoft, selectedLabelColor = YtOrange),
                        modifier = Modifier.testTag("np_sleep_btn"),
                    )
                    FilterChip(
                        selected = playerState.playbackSpeed != 1.0f || playerState.playbackPitch != 1.0f || showSpeedPanel,
                        onClick = { showSpeedPanel = !showSpeedPanel },
                        leadingIcon = { Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text(String.format(Locale.US, "%.2fx", playerState.playbackSpeed), maxLines = 1) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = YtOrangeSoft, selectedLabelColor = YtOrange),
                        modifier = Modifier.testTag("np_speed_btn"),
                    )
                    if (playerState.canSwitchToVideo) {
                        FilterChip(
                            selected = playerState.mode == PlayerMode.YOUTUBE_IFRAME,
                            onClick = onSwitchAudioVideo,
                            leadingIcon = {
                                Icon(
                                    imageVector = if (playerState.mode == PlayerMode.YOUTUBE_IFRAME) Icons.Default.Headphones else Icons.Default.OndemandVideo,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                            label = {
                                Text(if (playerState.mode == PlayerMode.YOUTUBE_IFRAME) "Ke Audio 8D" else "Mode Video", maxLines = 1)
                            },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = YtOrangeSoft, selectedLabelColor = YtOrange),
                            modifier = Modifier.testTag("np_av_switch_btn"),
                        )
                    }
                    FilterChip(
                        selected = false,
                        onClick = {
                            onDismiss()
                            onOpen8DStudio()
                        },
                        leadingIcon = { Icon(Icons.Default.SpatialAudioOff, contentDescription = null, tint = SpatialGreen, modifier = Modifier.size(16.dp)) },
                        label = { Text("8D Studio", maxLines = 1) },
                        modifier = Modifier.testTag("np_8d_studio_btn"),
                    )
                }

                // Expandable Sleep Timer Controls
                if (showSleepDialog) {
                    Surface(
                        color = YtCard,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, YtDivider),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Sleep Timer (Mi Music / YMusic)", style = MaterialTheme.typography.titleSmall, color = YtTextPrimary, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                listOf(15, 30, 60).forEach { mins ->
                                    OutlinedButton(onClick = { onSetSleepMinutes(mins); showSleepDialog = false }, shape = RoundedCornerShape(10.dp)) {
                                        Text("${mins}m", color = YtTextPrimary)
                                    }
                                }
                                OutlinedButton(onClick = { onSetSleepAfterTrack(); showSleepDialog = false }, shape = RoundedCornerShape(10.dp)) {
                                    Text("Akhir Lagu", color = YtOrange)
                                }
                                TextButton(onClick = { onCancelSleepTimer(); showSleepDialog = false }) {
                                    Text("Matikan", color = YtTextMuted)
                                }
                            }
                        }
                    }
                }

                // Expandable Speed & Pitch Controls
                if (showSpeedPanel) {
                    Surface(
                        color = YtCard,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, YtDivider),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Kecepatan & Pitch Audio", style = MaterialTheme.typography.titleSmall, color = YtTextPrimary, fontWeight = FontWeight.Bold)
                                TextButton(onClick = { onSetSpeedAndPitch(1.0f, 1.0f) }) {
                                    Text("Reset 1.0x", color = YtOrange)
                                }
                            }
                            Text("Kecepatan: ${String.format(Locale.US, "%.2fx", playerState.playbackSpeed)}", style = MaterialTheme.typography.bodySmall, color = YtTextSecondary)
                            Slider(
                                value = playerState.playbackSpeed,
                                onValueChange = { onSetSpeedAndPitch(it, playerState.playbackPitch) },
                                valueRange = 0.5f..2.0f,
                                colors = SliderDefaults.colors(thumbColor = YtOrange, activeTrackColor = YtOrange),
                            )
                            Text("Pitch: ${String.format(Locale.US, "%.2fx", playerState.playbackPitch)}", style = MaterialTheme.typography.bodySmall, color = YtTextSecondary)
                            Slider(
                                value = playerState.playbackPitch,
                                onValueChange = { onSetSpeedAndPitch(playerState.playbackSpeed, it) },
                                valueRange = 0.6f..1.5f,
                                colors = SliderDefaults.colors(thumbColor = SpatialGreen, activeTrackColor = SpatialGreen),
                            )
                        }
                    }
                }

                HorizontalDivider(color = YtDivider)

                // Sub-tabs: Queue | Lyrics | YouTube Comments & Like
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    NowPlayingTab.entries.forEach { tab ->
                        val selected = tab == activeTab
                        FilterChip(
                            selected = selected,
                            onClick = {
                                activeTab = tab
                                val vid = track.videoId
                                if (!vid.isNullOrBlank()) {
                                    if (tab == NowPlayingTab.LYRICS) onLoadLyrics(vid)
                                    if (tab == NowPlayingTab.SOCIAL) onLoadComments(vid)
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = when (tab) {
                                        NowPlayingTab.QUEUE -> Icons.AutoMirrored.Filled.QueueMusic
                                        NowPlayingTab.LYRICS -> Icons.Default.Lyrics
                                        NowPlayingTab.SOCIAL -> Icons.Default.Comment
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                            label = {
                                val countSuffix = if (tab == NowPlayingTab.QUEUE) " (${playerState.queue.size})" else ""
                                Text("${tab.label}$countSuffix", maxLines = 1)
                            },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = YtOrangeSoft, selectedLabelColor = YtOrange),
                            modifier = Modifier.testTag("np_tab_${tab.name.lowercase()}"),
                        )
                    }
                }

                when (activeTab) {
                    NowPlayingTab.QUEUE -> {
                        if (playerState.queue.isEmpty()) {
                            Text("Antrean kosong", style = MaterialTheme.typography.bodySmall, color = YtTextMuted)
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                playerState.queue.forEachIndexed { idx, qTrack ->
                                    val isCurrent = idx == playerState.currentIndex
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isCurrent) YtOrangeSoft else YtCard)
                                            .clickable { onSelectQueueIndex(idx) }
                                            .padding(horizontal = 10.dp, vertical = 8.dp)
                                            .testTag("np_queue_item_$idx"),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        Text(
                                            text = "${idx + 1}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (isCurrent) YtOrange else YtTextMuted,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = qTrack.title,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isCurrent) YtOrange else YtTextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            Text(
                                                text = qTrack.artist,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = YtTextSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                        if (playerState.queue.size > 1) {
                                            IconButton(
                                                onClick = { onRemoveQueueIndex(idx) },
                                                modifier = Modifier.minimumInteractiveComponentSize(),
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Hapus dari antrean", tint = YtTextMuted, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    NowPlayingTab.LYRICS -> {
                        Surface(
                            color = YtCard,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, YtDivider),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                when {
                                    track.videoId.isNullOrBlank() -> Text(
                                        "Lirik tersedia untuk lagu YouTube / YouTube Music.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = YtTextSecondary,
                                    )
                                    lyricsState.isLoading -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        CircularProgressIndicator(color = YtOrange, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                        Text("Memuat lirik…", style = MaterialTheme.typography.bodySmall, color = YtTextSecondary)
                                    }
                                    lyricsState.text.isNotBlank() -> Text(
                                        text = lyricsState.text,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = YtTextPrimary,
                                    )
                                    else -> {
                                        Text(
                                            text = lyricsState.error ?: "Lirik tidak tersedia.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = YtTextSecondary,
                                        )
                                        OutlinedButton(onClick = { onLoadLyrics(track.videoId) }, shape = RoundedCornerShape(10.dp)) {
                                            Text("Muat Ulang Lirik", color = YtOrange)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    NowPlayingTab.SOCIAL -> {
                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (track.videoId.isNullOrBlank()) {
                                Text("Fitur Like & Komentar tersedia untuk video/musik YouTube.", style = MaterialTheme.typography.bodySmall, color = YtTextSecondary)
                            } else {
                                // Like & Share action bar
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    val liked = commentsState.isLikedByMe == true
                                    OutlinedButton(
                                        onClick = {
                                            if (isLoggedIn) onToggleLikeYoutube() else onOpenLogin()
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, if (liked) YtOrange else YtDivider),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("np_yt_like_btn"),
                                    ) {
                                        Icon(
                                            imageVector = if (liked) Icons.Default.ThumbUp else Icons.Outlined.ThumbUp,
                                            contentDescription = "Suka di YouTube",
                                            tint = if (liked) YtOrange else YtTextPrimary,
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        val countSuffix = commentsState.likeCountText.takeIf { it.isNotBlank() }?.let { " ($it)" }.orEmpty()
                                        Text(
                                            text = (if (liked) "Disukai" else "Suka") + countSuffix,
                                            color = if (liked) YtOrange else YtTextPrimary,
                                            maxLines = 1,
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = { onShareTrack(track) },
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, YtDivider),
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null, tint = YtTextPrimary, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Bagikan", color = YtTextPrimary, maxLines = 1)
                                    }
                                }

                                // Comment composer
                                if (isLoggedIn) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        OutlinedTextField(
                                            value = commentDraft,
                                            onValueChange = { commentDraft = it },
                                            placeholder = { Text("Tulis komentar YouTube…", color = YtTextMuted) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("np_comment_input"),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = YtOrange,
                                                unfocusedBorderColor = YtDivider,
                                                focusedContainerColor = YtElevated,
                                                unfocusedContainerColor = YtElevated,
                                                focusedTextColor = YtTextPrimary,
                                                unfocusedTextColor = YtTextPrimary,
                                            ),
                                        )
                                        Button(
                                            onClick = {
                                                val txt = commentDraft.trim()
                                                if (txt.isNotEmpty()) {
                                                    onSubmitComment(txt)
                                                    commentDraft = ""
                                                }
                                            },
                                            enabled = !commentPosting && commentDraft.isNotBlank(),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = YtOrange, contentColor = Color.White),
                                            modifier = Modifier
                                                .height(52.dp)
                                                .testTag("np_comment_send_btn"),
                                        ) {
                                            if (commentPosting) {
                                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                                            } else {
                                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Kirim komentar")
                                            }
                                        }
                                    }
                                } else {
                                    Surface(
                                        color = YtCard,
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, YtDivider),
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                        ) {
                                            Text(
                                                "Login Google untuk Like & Kirim Komentar",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = YtTextSecondary,
                                                modifier = Modifier.weight(1f),
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Button(
                                                onClick = onOpenLogin,
                                                colors = ButtonDefaults.buttonColors(containerColor = YtOrange, contentColor = Color.White),
                                                shape = RoundedCornerShape(10.dp),
                                            ) {
                                                Text("Login")
                                            }
                                        }
                                    }
                                }

                                // Comment list
                                if (commentsLoading && commentsState.comments.isEmpty()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp),
                                        horizontalArrangement = Arrangement.Center,
                                    ) {
                                        CircularProgressIndicator(color = YtOrange, modifier = Modifier.size(22.dp))
                                    }
                                } else if (commentsState.comments.isEmpty()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text("Belum ada komentar dimuat", style = MaterialTheme.typography.bodySmall, color = YtTextMuted)
                                        TextButton(onClick = { onLoadComments(track.videoId) }) {
                                            Text("Muat Komentar", color = YtOrange)
                                        }
                                    }
                                } else {
                                    commentsState.comments.forEach { comment ->
                                        Surface(
                                            color = YtCard,
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, YtDivider),
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                                    Text(comment.author, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = YtOrange)
                                                    if (comment.publishedTime.isNotBlank()) {
                                                        Text(comment.publishedTime, style = MaterialTheme.typography.labelSmall, color = YtTextMuted)
                                                    }
                                                }
                                                Text(comment.text, style = MaterialTheme.typography.bodySmall, color = YtTextPrimary)
                                                if (comment.likeCount.isNotBlank()) {
                                                    Text("👍 ${comment.likeCount}", style = MaterialTheme.typography.labelSmall, color = YtTextMuted)
                                                }
                                            }
                                        }
                                    }
                                    if (!commentsState.continuationToken.isNullOrBlank()) {
                                        OutlinedButton(
                                            onClick = onLoadMoreComments,
                                            enabled = !commentsLoading,
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(10.dp),
                                        ) {
                                            Text(if (commentsLoading) "Memuat…" else "Muat Komentar Lainnya", color = YtTextPrimary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatSecTime(totalSec: Int): String {
    val s = totalSec.coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    val rem = s % 60
    return if (h > 0) {
        String.format(Locale.US, "%d:%02d:%02d", h, m, rem)
    } else {
        String.format(Locale.US, "%d:%02d", m, rem)
    }
}
