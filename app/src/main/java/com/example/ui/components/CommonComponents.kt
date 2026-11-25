package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.DownloadFormat
import com.example.data.Track
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.BgElevated
import com.example.ui.theme.BgHover
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

fun formatDuration(seconds: Int): String {
    if (seconds <= 0) return ""
    val m = seconds / 60
    val s = seconds % 60
    return "$m:${s.toString().padStart(2, '0')}"
}

@Composable
fun TrackRowItem(
    track: Track,
    onPlay: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onPlay)
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .testTag("track_item_${track.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(BgElevated),
            contentAlignment = Alignment.Center,
        ) {
            if (track.thumbnail.isNotBlank()) {
                AsyncImage(
                    model = track.thumbnail,
                    contentDescription = track.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(48.dp),
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            val dur = formatDuration(track.duration)
            Text(
                text = if (dur.isNotEmpty()) "${track.artist} · $dur" else track.artist,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        IconButton(
            onClick = onMore,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("track_more_${track.id}"),
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Opsi lagu",
                tint = TextSecondary,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackOptionsBottomSheet(
    track: Track,
    inLibrary: Boolean,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onToggleLibrary: () -> Unit,
    onDownload: (DownloadFormat) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = BgElevated,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
        ) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
            Text(
                text = track.artist,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            )
            Spacer(Modifier.height(12.dp))

            SheetActionRow(
                icon = Icons.Default.PlayArrow,
                label = "Putar",
                tint = AccentGreen,
                onClick = {
                    onPlay()
                    onDismiss()
                },
                testTag = "sheet_play_btn",
            )
            SheetActionRow(
                icon = if (inLibrary) Icons.Default.Delete else Icons.Default.LibraryAdd,
                label = if (inLibrary) "Hapus dari Library" else "Tambah ke Library",
                onClick = {
                    onToggleLibrary()
                    onDismiss()
                },
                testTag = "sheet_library_btn",
            )
            SheetActionRow(
                icon = Icons.Default.Download,
                label = "Download MP3",
                onClick = {
                    onDownload(DownloadFormat.MP3)
                    onDismiss()
                },
                testTag = "sheet_dl_mp3_btn",
            )
            SheetActionRow(
                icon = Icons.Default.Download,
                label = "Download MP4",
                onClick = {
                    onDownload(DownloadFormat.MP4)
                    onDismiss()
                },
                testTag = "sheet_dl_mp4_btn",
            )
        }
    }
}

@Composable
private fun SheetActionRow(
    icon: ImageVector,
    label: String,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    testTag: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge, color = tint)
    }
}

@Composable
fun EmptyStateView(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(BgHover),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = TextMuted, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = TextSecondary,
        )
        if (!subtitle.isNullOrBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
            )
        }
    }
}
