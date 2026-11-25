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
import com.example.ui.theme.DangerRed
import com.example.ui.theme.YtCard
import com.example.ui.theme.YtChipBg
import com.example.ui.theme.YtDivider
import com.example.ui.theme.YtElevated
import com.example.ui.theme.YtOrange
import com.example.ui.theme.YtTextMuted
import com.example.ui.theme.YtTextPrimary
import com.example.ui.theme.YtTextSecondary

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
                colors = CardDefaults.cardColors(containerColor = YtCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, YtDivider),
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_downloader_hero),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .matchParentSize()
                            .alpha(0.10f),
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, YtCard.copy(alpha = 0.94f)),
                                ),
                            ),
                    )
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "All Downloader",
                            style = MaterialTheme.typography.headlineSmall,
                            color = YtTextPrimary,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Paste link TikTok, YouTube, Instagram, X, Spotify, dll.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = YtTextSecondary,
                        )
                        Spacer(Modifier.height(16.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedTextField(
                                value = urlInput,
                                onValueChange = onUrlChange,
                                placeholder = { Text("Tempel URL di sini…", color = YtTextMuted) },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("url_input"),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = YtOrange,
                                    unfocusedBorderColor = YtDivider,
                                    focusedContainerColor = YtElevated,
                                    unfocusedContainerColor = YtElevated,
                                    focusedTextColor = YtTextPrimary,
                                    unfocusedTextColor = YtTextPrimary,
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
                                            Icon(Icons.Default.Clear, contentDescription = "Hapus URL", tint = YtTextMuted)
                                        }
                                    }
                                },
                            )

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = YtElevated,
                                border = androidx.compose.foundation.BorderStroke(1.dp, YtDivider),
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
                                        tint = YtTextSecondary,
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
                                containerColor = YtOrange,
                                contentColor = Color.White,
                            ),
                        ) {
                            if (scrapeBusy) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White,
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
                                        .background(YtChipBg)
                                        .border(1.dp, YtDivider, RoundedCornerShape(999.dp))
                                        .padding(horizontal = 10.dp, vertical = 5.dp),
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = YtTextSecondary,
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
                        color = YtTextPrimary,
                    )
                    TextButton(onClick = onClearFinishedJobs) {
                        Text("Bersihkan", color = YtOrange)
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
        colors = CardDefaults.cardColors(containerColor = YtCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, YtDivider),
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
                        .background(YtElevated),
                )
                Spacer(Modifier.height(12.dp))
            }

            Text(
                text = result.platformLabel.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = YtOrange,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = result.title.ifBlank { "(tanpa judul)" },
                style = MaterialTheme.typography.titleMedium,
                color = YtTextPrimary,
            )
            if (!result.author.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = result.author,
                    style = MaterialTheme.typography.bodySmall,
                    color = YtTextSecondary,
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
                    border = androidx.compose.foundation.BorderStroke(1.dp, YtDivider),
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = YtOrange, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "${item.type.uppercase()}$qualityText$extText",
                        color = YtTextPrimary,
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
                        border = androidx.compose.foundation.BorderStroke(1.dp, YtDivider),
                    ) {
                        Icon(Icons.Default.LibraryAdd, contentDescription = null, tint = YtTextPrimary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Library", color = YtTextPrimary, maxLines = 1)
                    }
                    Button(
                        onClick = onPlay,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("scrape_play_btn"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = YtOrange, contentColor = Color.White),
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
        colors = CardDefaults.cardColors(containerColor = YtCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, YtDivider),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = job.track.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = YtTextPrimary,
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
                        color = if (job.status == DownloadStatus.ERROR) DangerRed else YtTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Hapus unduhan", tint = YtTextMuted)
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
                    color = YtOrange,
                    trackColor = YtDivider,
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
