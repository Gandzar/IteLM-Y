package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.data.AppRepository
import com.example.data.AppSettings
import com.example.data.ReleaseInfo
import com.example.data.ScraprMeta
import com.example.data.ScraprUpdateInfo
import com.example.engine.MediaScraper
import com.example.engine.MusicSearchEngine
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.BgCard
import com.example.ui.theme.BgElevated
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.DateFormat
import java.util.Date

@Composable
fun SettingsScreen(
    settings: AppSettings,
    scraprMeta: ScraprMeta?,
    updateInfo: ReleaseInfo?,
    updateChecking: Boolean,
    scraprInfo: ScraprUpdateInfo?,
    scraprBusy: Boolean,
    scraprMsg: String,
    engineBusy: Boolean,
    engineMsg: String,
    onCheckAppUpdate: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onToggleAutoCheckApp: (Boolean) -> Unit,
    onCheckScraprUpdate: () -> Unit,
    onUpdateScrapr: () -> Unit,
    onRevertScrapr: () -> Unit,
    onToggleAutoUpdateEngine: (Boolean) -> Unit,
    onDownloadCustomEngine: (String) -> Unit,
    onRevertCustomEngine: () -> Unit,
    onDownloadFolderChange: (String) -> Unit,
    onYoutubeApiKeyChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var engineUrlDraft by remember(settings.customEngineUrl) { mutableStateOf(settings.customEngineUrl) }
    var folderDraft by remember(settings.downloadFolder) { mutableStateOf(settings.downloadFolder) }
    var ytKeyDraft by remember(settings.youtubeApiKey) { mutableStateOf(settings.youtubeApiKey) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            SectionLabel("Update Aplikasi", Modifier.widthIn(max = 640.dp))
            SettingsCard(Modifier.widthIn(max = 640.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("Versi saat ini", style = MaterialTheme.typography.titleSmall)
                        Text("v${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    Button(
                        onClick = onCheckAppUpdate,
                        enabled = !updateChecking,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = Color(0xFF041409)),
                        modifier = Modifier.testTag("btn_check_app_update"),
                    ) {
                        Text(if (updateChecking) "Cek…" else "Cek Update", fontWeight = FontWeight.SemiBold)
                    }
                }

                if (updateInfo != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = if (updateInfo.isNewer) "Update tersedia: v${updateInfo.tag}" else "Sudah versi terbaru",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (updateInfo.isNewer) AccentGreen else TextSecondary,
                    )
                    if (updateInfo.isNewer) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { onOpenUrl(updateInfo.htmlUrl) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                        ) {
                            Text("Buka halaman Release (v${updateInfo.tag})", color = TextPrimary)
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                CheckboxRow(
                    label = "Cek update otomatis saat buka app",
                    checked = settings.autoCheckAppUpdate,
                    onCheckedChange = onToggleAutoCheckApp,
                )
            }
        }

        item {
            SectionLabel("Engine / Scraper (scrapr)", Modifier.widthIn(max = 640.dp))
            SettingsCard(Modifier.widthIn(max = 640.dp)) {
                val currentScraprVer = scraprMeta?.version ?: AppRepository.BUILTIN_VERSION
                val source = scraprMeta?.source ?: "builtin"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Versi scraper", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = buildString {
                                append("Sekarang: $currentScraprVer")
                                if (scraprInfo != null) append(" · Terbaru: v${scraprInfo.latest}")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(source, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        scraprMeta?.updatedAt?.let { ts ->
                            Text(
                                DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(ts)),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                            )
                        }
                    }
                }

                if (scraprInfo != null) {
                    Spacer(Modifier.height(10.dp))
                    if (scraprInfo.isNewer) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(AccentGreen.copy(alpha = 0.14f))
                                .padding(10.dp),
                        ) {
                            Text(
                                "Update tersedia: v${scraprInfo.latest} dari ${scraprInfo.publishedVia}",
                                style = MaterialTheme.typography.bodySmall,
                                color = AccentGreen,
                                fontWeight = FontWeight.SemiBold,
                            )
                            if (!scraprInfo.description.isNullOrBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    scraprInfo.description.take(120),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                )
                            }
                        }
                    } else {
                        Text(
                            "Sudah versi terbaru (v${scraprInfo.latest})",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onCheckScraprUpdate,
                        enabled = !scraprBusy,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_check_scrapr"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = Color(0xFF041409)),
                    ) {
                        Text(if (scraprBusy) "Proses…" else "Cek Update", maxLines = 1)
                    }
                    Button(
                        onClick = onUpdateScrapr,
                        enabled = !scraprBusy,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_update_scrapr"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (scraprInfo?.isNewer == true) AccentGreen else BgElevated,
                            contentColor = if (scraprInfo?.isNewer == true) Color(0xFF041409) else TextPrimary,
                        ),
                    ) {
                        Text(if (scraprBusy) "Mengunduh…" else "Update Sekarang", maxLines = 1)
                    }
                }

                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onRevertScrapr,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                    ) {
                        Text("Kembalikan", color = TextPrimary, maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = { onOpenUrl(AppRepository.SCRAPR_REPO_URL) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                    ) {
                        Text("GitHub scrapr", color = TextPrimary, maxLines = 1)
                    }
                }

                if (scraprMsg.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(scraprMsg, style = MaterialTheme.typography.bodySmall, color = AccentGreen)
                }
            }
        }

        item {
            SectionLabel("Engine custom (URL manual)", Modifier.widthIn(max = 640.dp))
            SettingsCard(Modifier.widthIn(max = 640.dp)) {
                CheckboxRow(
                    label = "Cek update engine otomatis",
                    checked = settings.autoUpdateEngine,
                    onCheckedChange = onToggleAutoUpdateEngine,
                )
                Spacer(Modifier.height(8.dp))
                Text("URL konfigurasi engine custom (HTTPS)", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = engineUrlDraft,
                    onValueChange = { engineUrlDraft = it },
                    placeholder = { Text("https://raw.githubusercontent.com/.../package.json", color = TextMuted) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentGreen,
                        unfocusedBorderColor = BorderSubtle,
                        focusedContainerColor = BgElevated,
                        unfocusedContainerColor = BgElevated,
                    ),
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onDownloadCustomEngine(engineUrlDraft) },
                        enabled = !engineBusy,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = Color(0xFF041409)),
                    ) {
                        Text(if (engineBusy) "Mengunduh…" else "Unduh dari URL", maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = {
                            engineUrlDraft = ""
                            onRevertCustomEngine()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                    ) {
                        Text("Revert", color = TextPrimary, maxLines = 1)
                    }
                }
                if (engineMsg.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(engineMsg, style = MaterialTheme.typography.bodySmall, color = AccentGreen)
                }
            }
        }

        item {
            SectionLabel("Download & Search", Modifier.widthIn(max = 640.dp))
            SettingsCard(Modifier.widthIn(max = 640.dp)) {
                Text("Folder Download (Android Documents)", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = folderDraft,
                    onValueChange = {
                        folderDraft = it
                        onDownloadFolderChange(it)
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentGreen,
                        unfocusedBorderColor = BorderSubtle,
                        focusedContainerColor = BgElevated,
                        unfocusedContainerColor = BgElevated,
                    ),
                )
                Spacer(Modifier.height(12.dp))
                Text("YouTube Data API v3 Key (opsional)", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = ytKeyDraft,
                    onValueChange = {
                        ytKeyDraft = it
                        onYoutubeApiKeyChange(it)
                    },
                    placeholder = { Text("Kosong = mode demo search", color = TextMuted) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentGreen,
                        unfocusedBorderColor = BorderSubtle,
                        focusedContainerColor = BgElevated,
                        unfocusedContainerColor = BgElevated,
                    ),
                )
            }
        }

        item {
            SectionLabel("Tentang", Modifier.widthIn(max = 640.dp))
            SettingsCard(Modifier.widthIn(max = 640.dp)) {
                Text(
                    "YMusic Lite — All Downloader & Kruzen 8D Spatial Audio Studio.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "App v${BuildConfig.VERSION_NAME} · Engine ${MusicSearchEngine.ENGINE_VERSION} · Scraper ${MediaScraper.SCRAPER_VERSION}",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted,
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
    )
}

@Composable
private fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
private fun CheckboxRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = AccentGreen),
        )
        Text(text = label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 6.dp))
    }
}
