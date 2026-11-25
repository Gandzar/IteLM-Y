package com.example.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.Track
import com.example.ui.components.EmptyStateView
import com.example.ui.components.YouTubeFeedRowItem
import com.example.ui.theme.YtDivider
import com.example.ui.theme.YtElevated
import com.example.ui.theme.YtOrange
import com.example.ui.theme.YtTextMuted
import com.example.ui.theme.YtTextPrimary
import com.example.ui.theme.YtTextSecondary

@Composable
fun SearchScreen(
    query: String,
    results: List<Track>,
    isSearching: Boolean,
    isDemo: Boolean,
    onQueryChange: (String) -> Unit,
    onPlayTrack: (Track) -> Unit,
    onMoreTrack: (Track) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Cari lagu atau video di YouTube…", color = YtTextMuted) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = YtTextMuted) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Bersihkan", tint = YtTextMuted)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(999.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = YtOrange,
                unfocusedBorderColor = YtDivider,
                focusedContainerColor = YtElevated,
                unfocusedContainerColor = YtElevated,
                focusedTextColor = YtTextPrimary,
                unfocusedTextColor = YtTextPrimary,
            ),
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .testTag("search_input"),
        )

        if (isSearching) {
            Spacer(Modifier.height(32.dp))
            CircularProgressIndicator(color = YtOrange)
        } else if (results.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.Search,
                title = "Ketik di kotak search",
                subtitle = "Cari musik atau video YouTube (minimal 2 karakter)",
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (isDemo) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = YtElevated,
                            modifier = Modifier
                                .widthIn(max = 720.dp)
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = "Mode offline/demo (pencarian YouTube Music tidak terjangkau, atau atur YouTube API Key cadangan di Pengaturan).",
                                style = MaterialTheme.typography.bodySmall,
                                color = YtTextSecondary,
                                modifier = Modifier.padding(10.dp),
                            )
                        }
                    }
                }
                items(results, key = { it.id }) { track ->
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
