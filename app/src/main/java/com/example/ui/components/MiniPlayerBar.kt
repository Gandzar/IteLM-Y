package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.player.PlayerMode
import com.example.player.PlayerState
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.BgCard
import com.example.ui.theme.BgElevated
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

@Composable
fun MiniPlayerBar(
    playerState: PlayerState,
    onTogglePlayPause: () -> Unit,
    onToggleYoutubeVideo: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val track = playerState.track
    AnimatedVisibility(
        visible = track != null,
        enter = slideInVertically { it },
        exit = slideOutVertically { it },
        modifier = modifier,
    ) {
        if (track != null) {
            val progress = if (playerState.durationSec > 0f) {
                (playerState.currentTimeSec / playerState.durationSec).coerceIn(0f, 1f)
            } else 0f

            Surface(
                color = BgElevated,
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp),
                        color = AccentGreen,
                        trackColor = BorderSubtle,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = playerState.mode == PlayerMode.YOUTUBE_IFRAME) {
                                onToggleYoutubeVideo()
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(BgCard),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (track.thumbnail.isNotBlank()) {
                                AsyncImage(
                                    model = track.thumbnail,
                                    contentDescription = track.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(44.dp),
                                )
                            } else {
                                Icon(Icons.Default.MusicNote, contentDescription = null, tint = TextMuted)
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = track.artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        if (playerState.mode == PlayerMode.YOUTUBE_IFRAME) {
                            IconButton(
                                onClick = onToggleYoutubeVideo,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("player_yt_expand_btn"),
                            ) {
                                Icon(
                                    imageVector = if (playerState.youtubeExpanded) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                                    contentDescription = "Tampilkan atau sembunyikan video YouTube",
                                    tint = TextSecondary,
                                )
                            }
                        }

                        IconButton(
                            onClick = onTogglePlayPause,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("player_play_pause_btn"),
                        ) {
                            Icon(
                                imageVector = if (playerState.playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (playerState.playing) "Jeda" else "Putar",
                                tint = AccentGreen,
                            )
                        }

                        IconButton(
                            onClick = onStop,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("player_stop_btn"),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Tutup pemutar",
                                tint = TextMuted,
                            )
                        }
                    }
                }
            }
        }
    }
}
