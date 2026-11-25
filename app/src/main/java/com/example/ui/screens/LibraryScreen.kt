package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.Track
import com.example.ui.components.EmptyStateView
import com.example.ui.components.TrackRowItem
import com.example.ui.theme.TextMuted

@Composable
fun LibraryScreen(
    library: List<Track>,
    onPlayTrack: (Track) -> Unit,
    onMoreTrack: (Track) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (library.isEmpty()) {
        EmptyStateView(
            icon = Icons.Default.LibraryMusic,
            title = "Library kosong",
            subtitle = "Simpan lagu dari tab Download atau Search ke koleksi kamu.",
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
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "Library", style = MaterialTheme.typography.titleMedium)
                Text(text = "${library.size}", style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
        }
        items(library, key = { it.id }) { track ->
            TrackRowItem(
                track = track,
                onPlay = { onPlayTrack(track) },
                onMore = { onMoreTrack(track) },
                modifier = Modifier.widthIn(max = 640.dp),
            )
        }
    }
}
