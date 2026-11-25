package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.ui.theme.DangerRed
import com.example.ui.theme.SpatialBorder
import com.example.ui.theme.SpatialGreen
import com.example.ui.theme.SpatialMuted
import com.example.ui.theme.SpatialPanelAlt
import com.example.ui.theme.TextPrimary
import dev.bagja.spatialaudio.data.BUILTIN_PRESETS
import dev.bagja.spatialaudio.data.EQ_PRESETS
import dev.bagja.spatialaudio.data.SavedPreset
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpatialPresetsTabContent(
    presetNameInput: String,
    savedPresets: List<SavedPreset>,
    onNameChange: (String) -> Unit,
    onSavePreset: () -> Unit,
    onApplyBuiltin: (String) -> Unit,
    onApplyEqAndSwitch: (String) -> Unit,
    onLoadSaved: (SavedPreset) -> Unit,
    onDeleteSaved: (String) -> Unit,
    onExportJson: () -> String,
    onImportJsonUri: (Uri) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use {
                    it.write(onExportJson())
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) onImportJsonUri(uri)
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SpatialPanelCard(
            title = "Preset 8D bawaan",
            subtitle = "Profil gerakan dan karakter audio siap pakai",
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BUILTIN_PRESETS.forEach { preset ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0D141B))
                            .border(1.dp, Color(0xFF263641), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF173B31)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("✦", color = SpatialGreen)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(preset.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            Text(preset.description, style = MaterialTheme.typography.bodySmall, color = SpatialMuted)
                        }
                        OutlinedButton(
                            onClick = { onApplyBuiltin(preset.id) },
                            shape = RoundedCornerShape(9.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C5448)),
                        ) {
                            Text("TERAPKAN", style = MaterialTheme.typography.labelSmall, color = Color(0xFF83ECC0))
                        }
                    }
                }
            }
        }

        SpatialPanelCard(
            title = "Preset EQ",
            subtitle = "Kurva equalizer cepat berdasarkan karakter musik",
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                maxItemsInEachRow = 3,
            ) {
                EQ_PRESETS.forEach { p ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.31f)
                            .clip(RoundedCornerShape(11.dp))
                            .background(Color(0xFF0D141B))
                            .border(1.dp, Color(0xFF263641), RoundedCornerShape(11.dp))
                            .clickable { onApplyEqAndSwitch(p.label) }
                            .padding(10.dp),
                    ) {
                        Text(p.label, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Text(
                            p.gains.take(4).joinToString("·") {
                                val r = it.roundToInt()
                                if (r > 0) "+$r" else "$r"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = SpatialMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        SpatialPanelCard(
            title = "Preset saya",
            subtitle = "Simpan konfigurasi khusus dan gunakan lagi kapan saja",
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = presetNameInput,
                    onValueChange = onNameChange,
                    placeholder = { Text("Nama preset baru", color = SpatialMuted) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SpatialGreen,
                        unfocusedBorderColor = SpatialBorder,
                        focusedContainerColor = Color(0xFF0A1118),
                        unfocusedContainerColor = Color(0xFF0A1118),
                    ),
                )
                Button(
                    onClick = onSavePreset,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SpatialGreen, contentColor = Color(0xFF061710)),
                ) {
                    Text("Simpan", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(12.dp))
            if (savedPresets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, SpatialBorder, RoundedCornerShape(10.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Belum ada preset tersimpan.", style = MaterialTheme.typography.bodySmall, color = SpatialMuted)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    savedPresets.forEach { preset ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF0D141B))
                                .border(1.dp, Color(0xFF263641), RoundedCornerShape(12.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(preset.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                Text("Tersimpan di perangkat", style = MaterialTheme.typography.bodySmall, color = SpatialMuted)
                            }
                            OutlinedButton(
                                onClick = { onLoadSaved(preset) },
                                shape = RoundedCornerShape(9.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C5448)),
                            ) {
                                Text("MUAT", style = MaterialTheme.typography.labelSmall, color = SpatialGreen)
                            }
                            IconButton(
                                onClick = { onDeleteSaved(preset.id) },
                                modifier = Modifier.minimumInteractiveComponentSize(),
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Hapus preset", tint = DangerRed)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { exportLauncher.launch("kruzen-spatial-preset.json") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SpatialBorder),
                ) {
                    Text("Ekspor JSON", color = TextPrimary)
                }
                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SpatialBorder),
                ) {
                    Text("Impor JSON", color = TextPrimary)
                }
            }
        }
    }
}

@Composable
fun SpatialFloatingTabContent(
    floatingBubbleEnabled: Boolean,
    floatingLyricsEnabled: Boolean,
    floatingNotice: String,
    universalCaptureActive: Boolean,
    globalEqActive: Boolean,
    globalEqMessage: String?,
    onToggleBubble: (Boolean) -> Unit,
    onToggleLyrics: (Boolean) -> Unit,
    onToggleGlobalEq: (Boolean) -> Unit,
    onStartCapture: (Int, Intent?) -> Unit,
    onStopCapture: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val projectionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { res ->
        onStartCapture(res.resultCode, res.data)
    }

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val mgr = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            projectionLauncher.launch(mgr.createScreenCaptureIntent())
        }
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SpatialPanelCard(
            title = "Mode melayang",
            subtitle = "Kontrol tetap di atas aplikasi lain",
            badge = "ANDROID OVERLAY",
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF14352C))
                        .border(1.dp, SpatialGreen, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("8D", style = MaterialTheme.typography.titleLarge, color = SpatialGreen, fontWeight = FontWeight.ExtraBold)
                }
                Text(
                    "Aktifkan gelembung untuk mengganti preset, toggle 8D, atau mengontrol pemutaran ketika membuka aplikasi lain.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SpatialMuted,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        SpatialPanelCard(
            title = "Floating controls",
            subtitle = "Perlu izin “Tampil di atas aplikasi lain” dari Android",
        ) {
            SpatialSwitchRow(
                title = "Gelembung kontrol",
                subtitle = "Toggle 8D, preset EQ, preset 8D, Play/Pause",
                checked = floatingBubbleEnabled,
                onCheckedChange = onToggleBubble,
            )
            SpatialSwitchRow(
                title = "Lirik mengambang",
                subtitle = "Overlay lirik tersinkron dari host player",
                checked = floatingLyricsEnabled,
                onCheckedChange = onToggleLyrics,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = floatingNotice,
                style = MaterialTheme.typography.bodySmall,
                color = SpatialMuted,
            )
        }

        SpatialPanelCard(
            title = "Mode audio sistem",
            subtitle = "Jalur pemrosesan audio global, bila didukung perangkat",
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                    Text("Universal 8D Capture", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    Text(
                        "Capture audio aplikasi lain melalui MediaProjection (Android 10+).",
                        style = MaterialTheme.typography.bodySmall,
                        color = SpatialMuted,
                    )
                }
                Button(
                    onClick = {
                        if (universalCaptureActive) {
                            onStopCapture()
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            val hasPerm = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO,
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasPerm) {
                                val mgr = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                                projectionLauncher.launch(mgr.createScreenCaptureIntent())
                            } else {
                                recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    },
                    enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (universalCaptureActive) DangerRed else SpatialGreen,
                        contentColor = if (universalCaptureActive) Color.White else Color(0xFF061710),
                    ),
                ) {
                    Text(if (universalCaptureActive) "Matikan Capture" else "Mulai Capture", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                    Text("System Global EQ", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    Text(
                        "DynamicsProcessing session 0; bergantung dukungan vendor/ROM.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SpatialMuted,
                    )
                }
                Switch(
                    checked = globalEqActive,
                    onCheckedChange = onToggleGlobalEq,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF1D9E73),
                        uncheckedThumbColor = Color(0xFFD2DBE0),
                        uncheckedTrackColor = SpatialPanelAlt,
                    ),
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = globalEqMessage ?: "Mode audio sistem belum diaktifkan.",
                style = MaterialTheme.typography.bodySmall,
                color = SpatialGreen,
            )
        }
    }
}
