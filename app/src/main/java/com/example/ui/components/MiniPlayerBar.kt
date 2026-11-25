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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.CircularProgressIndicator
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
import com.example.ui.theme.YtCard
import com.example.ui.theme.YtDivider
import com.example.ui.theme.YtElevated
import com.example.ui.theme.YtOrange
import com.example.ui.theme.YtTextMuted
import com.example.ui.theme.YtTextPrimary
import com.example.ui.theme.YtTextSecondary

@Composable
fun MiniPlayerBar(
    playerState: PlayerState,
    onOpenNowPlaying: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onToggleAudioVideoMode: () -> Unit,
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
                color = YtCard,
                tonalElevation = 6.dp,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (playerState.resolvingStream) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp),
                            color = YtOrange,
                            trackColor = YtDivider,
                        )
                    } else {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp),
                            color = YtOrange,
                            trackColor = YtDivider,
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenNowPlaying() }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                            .testTag("mini_player_bar"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(YtElevated),
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
                                Icon(Icons.Default.MusicNote, contentDescription = null, tint = YtTextMuted)
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = YtTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(2.dp))
                            val subtitle = if (playerState.audioCodecLabel.isNotBlank()) {
                                "${track.artist} · ${playerState.audioCodecLabel}"
                            } else {
                                track.artist
                            }
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (playerState.mode == PlayerMode.AUDIO) YtOrange else YtTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        if (playerState.canSwitchToVideo) {
                            IconButton(
                                onClick = onToggleAudioVideoMode,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("player_mode_switch_btn"),
                            ) {
                                Icon(
                                    imageVector = if (playerState.mode == PlayerMode.YOUTUBE_IFRAME) {
                                        Icons.Default.Headphones
                                    } else {
                                        Icons.Default.OndemandVideo
                                    },
                                    contentDescription = if (playerState.mode == PlayerMode.YOUTUBE_IFRAME) {
                                        "Beralih ke Mode Audio 8D"
                                    } else {
                                        "Beralih ke Mode Video"
                                    },
                                    tint = if (playerState.mode == PlayerMode.AUDIO) YtTextSecondary else YtOrange,
                                )
                            }
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
                                    tint = YtTextSecondary,
                                )
                            }
                        }

                        if (playerState.resolvingStream) {
                            Box(
                                modifier = Modifier.minimumInteractiveComponentSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.5.dp,
                                    color = YtOrange,
                                )
                            }
                        } else {
                            IconButton(
                                onClick = onTogglePlayPause,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("player_play_pause_btn"),
                            ) {
                                Icon(
                                    imageVector = if (playerState.playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (playerState.playing) "Jeda" else "Putar",
                                    tint = YtOrange,
                                )
                            }
                        }

                        if (playerState.queue.size > 1) {
                            IconButton(
                                onClick = onNext,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("player_next_btn"),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = "Lagu berikutnya",
                                    tint = YtTextPrimary,
                                )
                            }
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
                                tint = YtTextMuted,
                            )
                        }
                    }
                }
            }
        }
    }
}
