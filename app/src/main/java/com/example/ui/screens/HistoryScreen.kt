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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.ScrapeResult
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.BgElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

@Composable
fun HistoryScreen(
    history: List<ScrapeResult>,
    onSelectHistory: (ScrapeResult) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (history.isEmpty()) {
        EmptyStateView(
            icon = Icons.Default.History,
            title = "Belum ada riwayat",
            subtitle = "Riwayat tautan yang berhasil diproses akan muncul di sini.",
            modifier = modifier,
        )
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Row(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "Riwayat (${history.size})", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onClearHistory) {
                    Text("Hapus Semua", color = AccentGreen)
                }
            }
        }
        itemsIndexed(history, key = { idx, item -> "${item.timestamp}_$idx" }) { idx, item ->
            Row(
                modifier = Modifier
                    .widthIn(max = 640.dp)
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
                        .background(BgElevated),
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
                        Icon(Icons.Default.Link, contentDescription = null, tint = TextMuted)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title.ifBlank { item.rawUrl },
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = item.platformLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
            }
        }
    }
}
