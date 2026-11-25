package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.ScraperServer
import com.example.data.ServerStatus
import com.example.ui.theme.DangerRed
import com.example.ui.theme.SpatialGreen
import com.example.ui.theme.YtCard
import com.example.ui.theme.YtDivider
import com.example.ui.theme.YtElevated
import com.example.ui.theme.YtOrange
import com.example.ui.theme.YtOrangeSoft
import com.example.ui.theme.YtTextMuted
import com.example.ui.theme.YtTextPrimary
import com.example.ui.theme.YtTextSecondary

@Composable
fun ServerMenuScreen(
    servers: List<ScraperServer>,
    activeServerId: String,
    isPinging: Boolean,
    onSelectServer: (String) -> Unit,
    onPingAll: () -> Unit,
    onAddCustomServer: (name: String, url: String) -> Unit,
    onRemoveCustomServer: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var customName by remember { mutableStateOf("") }
    var customUrl by remember { mutableStateOf("") }
    val active = servers.firstOrNull { it.id == activeServerId } ?: servers.firstOrNull()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Card(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = YtCard),
                border = BorderStroke(1.dp, YtOrange.copy(alpha = 0.45f)),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(YtOrangeSoft),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Default.Dns, contentDescription = null, tint = YtOrange)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Menu Server Scraper & YouTube",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = YtTextPrimary,
                            )
                            Text(
                                text = "Aktif: ${active?.name ?: "Auto Smart"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = YtOrange,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Text(
                        text = "Pilih jalur client YouTube (VR, iOS, Android, YT Music Login), Piped mirror eksternal, atau tambahkan API Cobalt/scrapr kustom HTTPS.",
                        style = MaterialTheme.typography.bodySmall,
                        color = YtTextSecondary,
                    )
                    Button(
                        onClick = onPingAll,
                        enabled = !isPinging,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("server_ping_all_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = YtOrange, contentColor = Color.White),
                    ) {
                        if (isPinging) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("Menguji Ping Server…", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Uji Ping Semua Server", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        items(servers, key = { it.id }) { server ->
            val selected = server.id == activeServerId
            ServerRowCard(
                server = server,
                selected = selected,
                onSelect = { onSelectServer(server.id) },
                onRemove = if (!server.isBuiltIn) ({ onRemoveCustomServer(server.id) }) else null,
                modifier = Modifier.widthIn(max = 680.dp),
            )
        }

        item {
            Card(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = YtCard),
                border = BorderStroke(1.dp, YtDivider),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Tambah Server Kustom (HTTPS)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = YtTextPrimary,
                    )
                    Text(
                        text = "Mendukung endpoint POST JSON (format Cobalt / scrapr) yang menerima {\"url\":\"...\"}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = YtTextSecondary,
                    )
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        placeholder = { Text("Nama Server (misal: Cobalt Pribadi)", color = YtTextMuted) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("server_custom_name_input"),
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
                    OutlinedTextField(
                        value = customUrl,
                        onValueChange = { customUrl = it },
                        placeholder = { Text("https://api.server-anda.com/api/json", color = YtTextMuted) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("server_custom_url_input"),
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
                    Button(
                        onClick = {
                            onAddCustomServer(customName, customUrl)
                            if (customUrl.trim().startsWith("https://")) {
                                customName = ""
                                customUrl = ""
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("server_add_custom_btn"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = YtElevated, contentColor = YtTextPrimary),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = YtOrange)
                        Spacer(Modifier.width(6.dp))
                        Text("Simpan & Pakai Server Kustom", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ServerRowCard(
    server: ScraperServer,
    selected: Boolean,
    onSelect: () -> Unit,
    onRemove: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val statusColor = when (server.lastStatus) {
        ServerStatus.ONLINE -> SpatialGreen
        ServerStatus.OFFLINE -> DangerRed
        ServerStatus.UNKNOWN -> YtTextMuted
    }
    val statusText = when (server.lastStatus) {
        ServerStatus.ONLINE -> server.lastPingMs?.let { "ONLINE · ${it}ms" } ?: "ONLINE"
        ServerStatus.OFFLINE -> "OFFLINE"
        ServerStatus.UNKNOWN -> "SIAP"
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = YtCard),
        border = BorderStroke(1.dp, if (selected) YtOrange else YtDivider),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = server.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = YtTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Surface(
                            color = statusColor.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(6.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(Icons.Default.Speed, contentDescription = null, tint = statusColor, modifier = Modifier.size(12.dp))
                                Text(
                                    text = statusText,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = statusColor,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = server.description.ifBlank { server.baseUrl },
                        style = MaterialTheme.typography.bodySmall,
                        color = YtTextSecondary,
                    )
                    Text(
                        text = "${server.kind.label}${if (server.supportsMultiPlatform) " · Multi-Platform" else " · YouTube"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = YtTextMuted,
                    )
                }

                if (onRemove != null) {
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("server_remove_${server.id}"),
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Hapus server", tint = DangerRed)
                    }
                }
            }

            if (selected) {
                Surface(
                    color = YtOrangeSoft,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = YtOrange, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Server Aktif Dipilih", color = YtOrange, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                    }
                }
            } else {
                OutlinedButton(
                    onClick = onSelect,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("server_select_${server.id}"),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, YtDivider),
                ) {
                    Text("Pakai Server Ini", color = YtTextPrimary, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
