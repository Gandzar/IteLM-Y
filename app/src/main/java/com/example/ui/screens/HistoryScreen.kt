package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.AccountProfile
import com.example.data.HistorySection
import com.example.data.ScrapeResult
import com.example.data.Track
import com.example.ui.components.EmptyStateView
import com.example.ui.components.YouTubeFeedRowItem
import com.example.ui.theme.YtDivider
import com.example.ui.theme.YtElevated
import com.example.ui.theme.YtOrange
import com.example.ui.theme.YtOrangeSoft
import com.example.ui.theme.YtTextMuted
import com.example.ui.theme.YtTextPrimary
import com.example.ui.theme.YtTextSecondary

@Composable
fun HistoryScreen(
    history: List<ScrapeResult>,
    accountProfile: AccountProfile,
    selectedSection: HistorySection,
    ytHistoryTracks: List<Track>,
    isSyncing: Boolean,
    onSelectSection: (HistorySection) -> Unit,
    onSyncYtHistory: () -> Unit,
    onLoginGoogle: () -> Unit,
    onSelectHistory: (ScrapeResult) -> Unit,
    onClearHistory: () -> Unit,
    onPlayTrack: (Track) -> Unit,
    onMoreTrack: (Track) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HistorySection.entries.forEach { section ->
                val selected = section == selectedSection
                FilterChip(
                    selected = selected,
                    onClick = { onSelectSection(section) },
                    label = { Text(section.label, maxLines = 1) },
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
                    modifier = Modifier.testTag("history_filter_${section.name.lowercase()}"),
                )
            }
            Spacer(Modifier.weight(1f))
            if (selectedSection == HistorySection.YOUTUBE_MUSIC && accountProfile.isLoggedIn) {
                IconButton(
                    onClick = onSyncYtHistory,
                    enabled = !isSyncing,
                    modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh riwayat YT Music", tint = YtOrange)
                }
            }
        }

        when (selectedSection) {
            HistorySection.SCRAPE -> {
                if (history.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Default.History,
                        title = "Belum ada riwayat scrape",
                        subtitle = "Riwayat tautan yang berhasil diproses akan muncul di sini.",
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        item {
                            Row(
                                modifier = Modifier
                                    .widthIn(max = 720.dp)
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "Riwayat (${history.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = YtTextPrimary,
                                )
                                TextButton(onClick = onClearHistory) {
                                    Text("Hapus Semua", color = YtOrange)
                                }
                            }
                        }
                        itemsIndexed(history, key = { idx, item -> "${item.timestamp}_$idx" }) { idx, item ->
                            Row(
                                modifier = Modifier
                                    .widthIn(max = 720.dp)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onSelectHistory(item) }
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                                    .testTag("history_item_$idx"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(YtElevated),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (!item.thumbnail.isNullOrBlank()) {
                                        AsyncImage(
                                            model = item.thumbnail,
                                            contentDescription = item.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.size(48.dp),
                                        )
                                    } else {
                                        Icon(Icons.Default.Link, contentDescription = null, tint = YtTextMuted)
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title.ifBlank { item.rawUrl },
                                        style = MaterialTheme.typography.titleSmall,
                                        color = YtTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = item.platformLabel,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = YtTextSecondary,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HistorySection.YOUTUBE_MUSIC -> {
                if (!accountProfile.isLoggedIn) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, tint = YtTextMuted, modifier = Modifier.size(44.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("Login untuk Riwayat YT Music", style = MaterialTheme.typography.titleMedium, color = YtTextPrimary)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Masuk dengan akun Google untuk memuat lagu yang terakhir kamu putar di YouTube Music.",
                            style = MaterialTheme.typography.bodySmall,
                            color = YtTextSecondary,
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = onLoginGoogle,
                            shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = YtOrange, contentColor = Color.White),
                        ) {
                            Text("Login Google / YouTube", fontWeight = FontWeight.SemiBold)
                        }
                    }
                } else if (isSyncing && ytHistoryTracks.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CircularProgressIndicator(color = YtOrange)
                        Spacer(Modifier.height(12.dp))
                        Text("Memuat riwayat YouTube Music…", style = MaterialTheme.typography.bodySmall, color = YtTextSecondary)
                    }
                } else if (ytHistoryTracks.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Default.History,
                        title = "Riwayat YT Music kosong",
                        subtitle = "Putar lagu atau ketuk refresh untuk menyinkronkan riwayat putar.",
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        items(ytHistoryTracks, key = { "ythist_${it.id}" }) { track ->
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
}
