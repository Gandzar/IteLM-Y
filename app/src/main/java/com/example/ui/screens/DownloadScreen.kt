package com.example.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.DownloadJob
import com.example.data.DownloadStatus
import com.example.data.MediaItem
import com.example.data.ScrapeResult
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.BgCard
import com.example.ui.theme.BgElevated
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.DangerRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DownloadScreen(
    urlInput: String,
    scrapeBusy: Boolean,
    lastScrape: ScrapeResult?,
    downloadJobs: List<DownloadJob>,
    onUrlChange: (String) -> Unit,
    onScrape: () -> Unit,
    onToast: (String) -> Unit,
    onDownloadItem: (MediaItem) -> Unit,
    onSaveToLibrary: () -> Unit,
    onPlayScrape: () -> Unit,
    onRemoveJob: (String) -> Unit,
    onClearFinishedJobs: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Card(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = BgCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_downloader_hero),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .matchParentSize()
                            .alpha(0.22f),
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, BgCard.copy(alpha = 0.92f)),
                                ),
                            ),
                    )
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "All Downloader",
                            style = MaterialTheme.typography.headlineSmall,
                            color = TextPrimary,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Paste link TikTok, YouTube, Instagram, X, Spotify, dll.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                        )
                        Spacer(Modifier.height(16.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedTextField(
                                value = urlInput,
                                onValueChange = onUrlChange,
                                placeholder = { Text("Tempel URL di sini…", color = TextMuted) },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("url_input"),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AccentGreen,
                                    unfocusedBorderColor = BorderSubtle,
                                    focusedContainerColor = BgElevated.copy(alpha = 0.9f),
                                    unfocusedContainerColor = BgElevated.copy(alpha = 0.9f),
                                ),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Uri,
                                    imeAction = ImeAction.Go,
                                ),
                                keyboardActions = KeyboardActions(
                                    onGo = {
                                        keyboard?.hide()
                                        onScrape()
                                    },
                                ),
                                trailingIcon = {
                                    if (urlInput.isNotEmpty()) {
                                        IconButton(onClick = { onUrlChange("") }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Hapus URL", tint = TextMuted)
                                        }
                                    }
                                },
                            )

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = BgElevated,
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                            ) {
                                IconButton(
                                    onClick = {
                                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clipText = cm.primaryClip?.getItemAt(0)?.text?.toString()?.trim()
                                        if (!clipText.isNullOrEmpty()) {
                                            onUrlChange(clipText)
                                            onToast("Ditempel")
                                        } else {
                                            onToast("Clipboard kosong")
                                        }
                                    },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("paste_url_btn"),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentPaste,
                                        contentDescription = "Tempel dari clipboard",
                                        tint = TextSecondary,
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        Button(
                            onClick = {
                                keyboard?.hide()
                                onScrape()
                            },
                            enabled = !scrapeBusy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("scrape_download_btn"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentGreen,
                                contentColor = Color(0xFF041409),
                            ),
                        ) {
                            if (scrapeBusy) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF041409),
                                )
                                Spacer(Modifier.width(10.dp))
                                Text("Memproses…", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Download", fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            listOf("YouTube", "TikTok", "Instagram", "X", "Spotify", "SoundCloud", "+more").forEach { label ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(BgElevated.copy(alpha = 0.9f))
                                        .border(1.dp, BorderSubtle, RoundedCornerShape(999.dp))
                                        .padding(horizontal = 10.dp, vertical = 5.dp),
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (lastScrape != null) {
            item {
                ScrapeResultCard(
                    result = lastScrape,
                    onDownloadItem = onDownloadItem,
                    onSaveToLibrary = onSaveToLibrary,
                    onPlay = onPlayScrape,
                    modifier = Modifier.widthIn(max = 640.dp),
                )
            }
        }

        if (downloadJobs.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .widthIn(max = 640.dp)
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Antrean Download (${downloadJobs.size})",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    TextButton(onClick = onClearFinishedJobs) {
                        Text("Bersihkan", color = AccentGreen)
                    }
                }
            }

            items(downloadJobs, key = { it.id }) { job ->
                DownloadJobRow(
                    job = job,
                    onRemove = { onRemoveJob(job.id) },
                    modifier = Modifier.widthIn(max = 640.dp),
                )
            }
        }
    }
}

@Composable
private fun ScrapeResultCard(
    result: ScrapeResult,
    onDownloadItem: (MediaItem) -> Unit,
    onSaveToLibrary: () -> Unit,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            if (!result.thumbnail.isNullOrBlank()) {
                AsyncImage(
                    model = result.thumbnail,
                    contentDescription = result.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(BgElevated),
                )
                Spacer(Modifier.height(12.dp))
            }

            Text(
                text = result.platformLabel.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = AccentGreen,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = result.title.ifBlank { "(tanpa judul)" },
                style = MaterialTheme.typography.titleMedium,
            )
            if (!result.author.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = result.author,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
            if (!result.error.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = result.error,
                    style = MaterialTheme.typography.bodySmall,
                    color = DangerRed,
                )
            }

            Spacer(Modifier.height(12.dp))

            result.items.forEachIndexed { idx, item ->
                val qualityText = item.quality?.let { " · $it" } ?: ""
                val extText = item.ext?.let { " ($it)" } ?: ""
                OutlinedButton(
                    onClick = { onDownloadItem(item) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .testTag("scrape_item_btn_$idx"),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "${item.type.uppercase()}$qualityText$extText",
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (result.status) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = onSaveToLibrary,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("scrape_save_library_btn"),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                    ) {
                        Icon(Icons.Default.LibraryAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Library", maxLines = 1)
                    }
                    Button(
                        onClick = onPlay,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("scrape_play_btn"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = Color(0xFF041409)),
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Putar", fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadJobRow(
    job: DownloadJob,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = job.track.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${job.format.label} · ${
                            when (job.status) {
                                DownloadStatus.QUEUED -> "Menunggu"
                                DownloadStatus.DOWNLOADING -> "Mengunduh ${job.progress}%"
                                DownloadStatus.CONVERTING -> "Menyimpan…"
                                DownloadStatus.DONE -> job.filePath ?: "Selesai"
                                DownloadStatus.ERROR -> "Gagal"
                            }
                        }",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (job.status == DownloadStatus.ERROR) DangerRed else TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Hapus unduhan", tint = TextMuted)
                }
            }
            if (job.status == DownloadStatus.DOWNLOADING || job.status == DownloadStatus.CONVERTING) {
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { (job.progress / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = AccentGreen,
                    trackColor = BorderSubtle,
                )
            }
            if (!job.error.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = job.error,
                    style = MaterialTheme.typography.bodySmall,
                    color = DangerRed,
                )
            }
        }
    }
}
