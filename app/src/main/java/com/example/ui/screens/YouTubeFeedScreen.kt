package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.HomeChip
import com.example.data.Track
import com.example.ui.components.YouTubeFeedRowItem
import com.example.ui.theme.YtChipBg
import com.example.ui.theme.YtDivider
import com.example.ui.theme.YtOrange
import com.example.ui.theme.YtTextPrimary

@Composable
fun YouTubeFeedScreen(
    tracks: List<Track>,
    isLoading: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit,
    onPlayTrack: (Track) -> Unit,
    onMoreTrack: (Track) -> Unit,
    modifier: Modifier = Modifier,
    showChips: Boolean = false,
    selectedChip: HomeChip = HomeChip.ALL,
    onSelectChip: ((HomeChip) -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (showChips && onSelectChip != null) {
            LazyRow(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(HomeChip.entries, key = { it.name }) { chip ->
                    val selected = chip == selectedChip
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) YtOrange else YtChipBg)
                            .border(
                                width = 1.dp,
                                color = if (selected) YtOrange else YtDivider,
                                shape = RoundedCornerShape(10.dp),
                            )
                            .clickable { onSelectChip(chip) }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                            .testTag("home_chip_${chip.name.lowercase()}"),
                    ) {
                        Text(
                            text = chip.label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selected) Color.White else YtTextPrimary,
                        )
                    }
                }
            }
        }

        when {
            isLoading && tracks.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = YtOrange)
                }
            }

            errorMessage != null && tracks.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.CloudOff,
                            contentDescription = null,
                            tint = Color(0xFFA0A0A0),
                            modifier = Modifier.size(60.dp),
                        )
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF9C3026),
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(18.dp))
                        Button(
                            onClick = onRetry,
                            shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = YtOrange,
                                contentColor = Color.White,
                            ),
                            contentPadding = PaddingValues(horizontal = 26.dp, vertical = 10.dp),
                            modifier = Modifier.testTag("feed_retry_btn"),
                        ) {
                            Text("Coba lagi", fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    items(tracks, key = { "${it.id}_${it.playlistBrowseId ?: ""}" }) { track ->
                        YouTubeFeedRowItem(
                            track = track,
                            onPlay = { onPlayTrack(track) },
                            onMore = { onMoreTrack(track) },
                            modifier = Modifier.widthIn(max = 720.dp),
                        )
                    }
                }
            }
        }
    }
}
