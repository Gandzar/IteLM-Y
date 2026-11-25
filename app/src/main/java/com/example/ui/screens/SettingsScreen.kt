package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.BuildConfig
import com.example.data.AccountProfile
import com.example.data.AppRepository
import com.example.data.AppSettings
import com.example.data.ReleaseInfo
import com.example.data.ScraprMeta
import com.example.data.ScraprUpdateInfo
import com.example.engine.MediaScraper
import com.example.engine.MusicSearchEngine
import com.example.ui.theme.DangerRed
import com.example.ui.theme.YtCard
import com.example.ui.theme.YtDivider
import com.example.ui.theme.YtElevated
import com.example.ui.theme.YtOrange
import com.example.ui.theme.YtOrangeSoft
import com.example.ui.theme.YtTextMuted
import com.example.ui.theme.YtTextPrimary
import com.example.ui.theme.YtTextSecondary
import java.text.DateFormat
import java.util.Date

@Composable
fun SettingsScreen(
    accountProfile: AccountProfile,
    settings: AppSettings,
    scraprMeta: ScraprMeta?,
    updateInfo: ReleaseInfo?,
    updateChecking: Boolean,
    scraprInfo: ScraprUpdateInfo?,
    scraprBusy: Boolean,
    scraprMsg: String,
    engineBusy: Boolean,
    engineMsg: String,
    onLoginGoogle: () -> Unit,
    onRefreshProfile: () -> Unit,
    onLogoutGoogle: () -> Unit,
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
            SectionLabel("Akun Google & YouTube Music", Modifier.widthIn(max = 640.dp))
            SettingsCard(Modifier.widthIn(max = 640.dp)) {
                if (accountProfile.isLoggedIn) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(YtElevated),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (accountProfile.avatarUrl.isNotBlank()) {
                                AsyncImage(
                                    model = accountProfile.avatarUrl,
                                    contentDescription = accountProfile.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(52.dp),
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = YtOrange,
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = accountProfile.name.ifBlank { "Akun YouTube Music" },
                                style = MaterialTheme.typography.titleMedium,
                                color = YtTextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = accountProfile.handle.ifBlank { "Sesi terhubung · Terenkripsi" },
                                style = MaterialTheme.typography.bodySmall,
                                color = YtOrange,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onRefreshProfile,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_google_sync"),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, YtDivider),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = YtTextPrimary,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Sinkronkan", color = YtTextPrimary, maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = onLogoutGoogle,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_google_logout"),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DangerRed.copy(alpha = 0.5f)),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = null,
                                tint = DangerRed,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Logout", color = DangerRed, maxLines = 1)
                        }
                    }
                } else {
                    Text(
                        text = "Belum login",
                        style = MaterialTheme.typography.titleSmall,
                        color = YtTextPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Masuk dengan akun Google untuk menyinkronkan Playlist pribadi, Liked Music, Riwayat YouTube Music, dan pencarian terautentikasi.",
                        style = MaterialTheme.typography.bodySmall,
                        color = YtTextSecondary,
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onLoginGoogle,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_google_login"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = YtOrange,
                            contentColor = Color.White,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Login,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Login Google / YouTube", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        item {
            SectionLabel("Update Aplikasi", Modifier.widthIn(max = 640.dp))
            SettingsCard(Modifier.widthIn(max = 640.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("Versi saat ini", style = MaterialTheme.typography.titleSmall, color = YtTextPrimary)
                        Text("v${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall, color = YtTextSecondary)
                    }
                    Button(
                        onClick = onCheckAppUpdate,
                        enabled = !updateChecking,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = YtOrange, contentColor = Color.White),
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
                        color = if (updateInfo.isNewer) YtOrange else YtTextSecondary,
                    )
                    if (updateInfo.isNewer) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { onOpenUrl(updateInfo.htmlUrl) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, YtDivider),
                        ) {
                            Text("Buka halaman Release (v${updateInfo.tag})", color = YtTextPrimary)
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
                        Text("Versi scraper", style = MaterialTheme.typography.titleSmall, color = YtTextPrimary)
                        Text(
                            text = buildString {
                                append("Sekarang: $currentScraprVer")
                                if (scraprInfo != null) append(" · Terbaru: v${scraprInfo.latest}")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = YtTextSecondary,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(source, style = MaterialTheme.typography.labelSmall, color = YtTextMuted)
                        scraprMeta?.updatedAt?.let { ts ->
                            Text(
                                DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(ts)),
                                style = MaterialTheme.typography.labelSmall,
                                color = YtTextMuted,
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
                                .background(YtOrangeSoft)
                                .padding(10.dp),
                        ) {
                            Text(
                                "Update tersedia: v${scraprInfo.latest} dari ${scraprInfo.publishedVia}",
                                style = MaterialTheme.typography.bodySmall,
                                color = YtOrange,
                                fontWeight = FontWeight.SemiBold,
                            )
                            if (!scraprInfo.description.isNullOrBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    scraprInfo.description.take(120),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = YtTextSecondary,
                                )
                            }
                        }
                    } else {
                        Text(
                            "Sudah versi terbaru (v${scraprInfo.latest})",
                            style = MaterialTheme.typography.bodySmall,
                            color = YtTextSecondary,
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
                        colors = ButtonDefaults.buttonColors(containerColor = YtOrange, contentColor = Color.White),
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
                            containerColor = if (scraprInfo?.isNewer == true) YtOrange else YtElevated,
                            contentColor = if (scraprInfo?.isNewer == true) Color.White else YtTextPrimary,
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
                        border = androidx.compose.foundation.BorderStroke(1.dp, YtDivider),
                    ) {
                        Text("Kembalikan", color = YtTextPrimary, maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = { onOpenUrl(AppRepository.SCRAPR_REPO_URL) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, YtDivider),
                    ) {
                        Text("GitHub scrapr", color = YtTextPrimary, maxLines = 1)
                    }
                }

                if (scraprMsg.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(scraprMsg, style = MaterialTheme.typography.bodySmall, color = YtOrange)
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
                Text("URL konfigurasi engine custom (HTTPS)", style = MaterialTheme.typography.bodySmall, color = YtTextSecondary)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = engineUrlDraft,
                    onValueChange = { engineUrlDraft = it },
                    placeholder = { Text("https://raw.githubusercontent.com/.../package.json", color = YtTextMuted) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = YtOrange,
                        unfocusedBorderColor = YtDivider,
                        focusedContainerColor = YtElevated,
                        unfocusedContainerColor = YtElevated,
                        focusedTextColor = YtTextPrimary,
                        unfocusedTextColor = YtTextPrimary,
                    ),
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onDownloadCustomEngine(engineUrlDraft) },
                        enabled = !engineBusy,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = YtOrange, contentColor = Color.White),
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
                        border = androidx.compose.foundation.BorderStroke(1.dp, YtDivider),
                    ) {
                        Text("Revert", color = YtTextPrimary, maxLines = 1)
                    }
                }
                if (engineMsg.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(engineMsg, style = MaterialTheme.typography.bodySmall, color = YtOrange)
                }
            }
        }

        item {
            SectionLabel("Download & Search", Modifier.widthIn(max = 640.dp))
            SettingsCard(Modifier.widthIn(max = 640.dp)) {
                Text("Folder Download (Android Documents)", style = MaterialTheme.typography.bodySmall, color = YtTextSecondary)
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
                        focusedBorderColor = YtOrange,
                        unfocusedBorderColor = YtDivider,
                        focusedContainerColor = YtElevated,
                        unfocusedContainerColor = YtElevated,
                        focusedTextColor = YtTextPrimary,
                        unfocusedTextColor = YtTextPrimary,
                    ),
                )
                Spacer(Modifier.height(12.dp))
                Text("YouTube Data API v3 Key (opsional fallback)", style = MaterialTheme.typography.bodySmall, color = YtTextSecondary)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = ytKeyDraft,
                    onValueChange = {
                        ytKeyDraft = it
                        onYoutubeApiKeyChange(it)
                    },
                    placeholder = { Text("Kosong = InnerTube YT Music langsung", color = YtTextMuted) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = YtOrange,
                        unfocusedBorderColor = YtDivider,
                        focusedContainerColor = YtElevated,
                        unfocusedContainerColor = YtElevated,
                        focusedTextColor = YtTextPrimary,
                        unfocusedTextColor = YtTextPrimary,
                    ),
                )
            }
        }

        item {
            SectionLabel("Tentang", Modifier.widthIn(max = 640.dp))
            SettingsCard(Modifier.widthIn(max = 640.dp)) {
                Text(
                    "YMusic Lite — YouTube Music Streamer, All Downloader & Kruzen 8D Spatial Audio Studio.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = YtTextSecondary,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "App v${BuildConfig.VERSION_NAME} · Engine ${MusicSearchEngine.ENGINE_VERSION} · Scraper ${MediaScraper.SCRAPER_VERSION}",
                    style = MaterialTheme.typography.labelMedium,
                    color = YtTextMuted,
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
        color = YtTextPrimary,
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
        colors = CardDefaults.cardColors(containerColor = YtCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, YtDivider),
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
            colors = CheckboxDefaults.colors(checkedColor = YtOrange, checkmarkColor = Color.White),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = YtTextPrimary,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}
