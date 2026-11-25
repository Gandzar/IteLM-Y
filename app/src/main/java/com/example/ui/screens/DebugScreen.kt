package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.YMusicApplication
import com.example.debug.AppDiagnostics
import com.example.debug.DebugActivity
import com.example.debug.DebugLogLevel
import com.example.notify.AppNotifications
import com.example.ui.theme.YtCard
import com.example.ui.theme.YtDivider
import com.example.ui.theme.YtElevated
import com.example.ui.theme.YtOrange
import com.example.ui.theme.YtOrangeSoft
import com.example.ui.theme.YtTextMuted
import com.example.ui.theme.YtTextPrimary
import com.example.ui.theme.YtTextSecondary
import dev.bagja.spatialaudio.data.MixerRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class DebugSectionTab(val label: String) {
    MEMORY("RAM & Sistem"),
    LIFECYCLE("Activity & Service"),
    AUDIO("Audio & 8D DSP"),
    LOGS("Live Logs"),
}

@Composable
fun DebugScreen(
    isStandaloneActivity: Boolean = false,
    onToast: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val memory by AppDiagnostics.memory.collectAsStateWithLifecycle()
    val summary by AppDiagnostics.summary.collectAsStateWithLifecycle()
    val logs by AppDiagnostics.logs.collectAsStateWithLifecycle()

    val app = remember { runCatching { YMusicApplication.instance }.getOrNull() }
    val playerState by (app?.player?.state ?: remember { kotlinx.coroutines.flow.MutableStateFlow(com.example.player.PlayerState()) })
        .collectAsStateWithLifecycle()
    val downloadJobs by (app?.downloadManager?.jobs ?: remember { kotlinx.coroutines.flow.MutableStateFlow(emptyList()) })
        .collectAsStateWithLifecycle()
    val authProfile by (app?.authManager?.profile ?: remember { kotlinx.coroutines.flow.MutableStateFlow(com.example.data.AccountProfile()) })
        .collectAsStateWithLifecycle()
    val mixerSettings by MixerRepository.settings.collectAsStateWithLifecycle()
    val panFrame by MixerRepository.frame.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(DebugSectionTab.MEMORY) }
    var levelFilter by remember { mutableStateOf<DebugLogLevel?>(null) }
    var logSearch by remember { mutableStateOf("") }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Section switch chips
        Row(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            DebugSectionTab.entries.forEach { tab ->
                val selected = tab == selectedTab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) YtOrange else YtElevated)
                        .clickable { selectedTab = tab }
                        .padding(vertical = 8.dp, horizontal = 4.dp)
                        .testTag("debug_tab_${tab.name.lowercase()}"),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) Color.White else YtTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (!isStandaloneActivity) {
                item {
                    Card(
                        modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = YtCard),
                        border = androidx.compose.foundation.BorderStroke(1.dp, YtDivider),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Debug Activity Mandiri", style = MaterialTheme.typography.titleSmall, color = YtTextPrimary)
                                Text("Buka di jendela Activity terpisah", style = MaterialTheme.typography.bodySmall, color = YtTextSecondary)
                            }
                            Button(
                                onClick = {
                                    context.startActivity(
                                        Intent(context, DebugActivity::class.java).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        },
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = YtOrange, contentColor = Color.White),
                                modifier = Modifier.testTag("open_debug_activity_btn"),
                            ) {
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Buka", maxLines = 1)
                            }
                        }
                    }
                }
            }

            when (selectedTab) {
                DebugSectionTab.MEMORY -> {
                    item {
                        Card(
                            modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = YtCard),
                            border = androidx.compose.foundation.BorderStroke(1.dp, YtDivider),
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Memory, contentDescription = null, tint = YtOrange, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Penggunaan RAM Real-Time", style = MaterialTheme.typography.titleMedium, color = YtTextPrimary)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(if (memory.isLowMemory) Color(0xFFFFEBEE) else Color(0xFFE8F5E9))
                                            .padding(horizontal = 8.dp, vertical = 3.dp),
                                    ) {
                                        Text(
                                            text = if (memory.isLowMemory) "LOW RAM" else "NORMAL",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (memory.isLowMemory) Color(0xFFC62828) else Color(0xFF2E7D32),
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }

                                // Live Sparkline Chart
                                Canvas(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(84.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF10151C))
                                        .padding(6.dp),
                                ) {
                                    val pts = memory.historyMb
                                    if (pts.size >= 2) {
                                        val maxVal = (pts.maxOrNull() ?: 64f).coerceAtLeast(32f) * 1.15f
                                        val path = Path()
                                        pts.forEachIndexed { idx, v ->
                                            val x = (idx.toFloat() / (pts.size - 1)) * size.width
                                            val y = size.height - ((v / maxVal).coerceIn(0f, 1f) * size.height)
                                            if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                                        }
                                        drawPath(
                                            path = path,
                                            color = Color(0xFFE6681B),
                                            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round),
                                        )
                                    } else {
                                        drawLine(
                                            color = Color(0xFF26323D),
                                            start = Offset(0f, size.height / 2f),
                                            end = Offset(size.width, size.height / 2f),
                                        )
                                    }
                                }

                                val jvmRatio = (memory.jvmUsedMb / memory.jvmMaxMb.coerceAtLeast(1f)).coerceIn(0f, 1f)
                                MetricProgressRow(
                                    label = "JVM Heap",
                                    valueText = String.format(Locale.US, "%.1f / %.1f MB (Max %.0f MB)", memory.jvmUsedMb, memory.jvmTotalMb, memory.jvmMaxMb),
                                    progress = jvmRatio,
                                )

                                val sysUsed = (memory.systemTotalMb - memory.systemAvailMb).coerceAtLeast(0f)
                                val sysRatio = (sysUsed / memory.systemTotalMb.coerceAtLeast(1f)).coerceIn(0f, 1f)
                                MetricProgressRow(
                                    label = "System RAM",
                                    valueText = String.format(Locale.US, "Tersedia %.0f / %.0f MB", memory.systemAvailMb, memory.systemTotalMb),
                                    progress = sysRatio,
                                )

                                Text(
                                    text = String.format(
                                        Locale.US,
                                        "Native Heap: %.1f / %.1f MB  ·  Total PSS: %.1f MB",
                                        memory.nativeAllocatedMb,
                                        memory.nativeTotalMb,
                                        memory.totalPssMb,
                                    ),
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                    color = YtTextSecondary,
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            AppDiagnostics.triggerGc()
                                            onToast("Garbage Collection dijalankan")
                                        },
                                        modifier = Modifier.weight(1f).testTag("debug_gc_btn"),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = YtOrange, contentColor = Color.White),
                                    ) {
                                        Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Force GC", maxLines = 1)
                                    }
                                    OutlinedButton(
                                        onClick = { AppDiagnostics.sampleMemoryNow() },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, YtDivider),
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, tint = YtTextPrimary, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Refresh", color = YtTextPrimary, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }

                    item {
                        DebugInfoCard(title = "Sistem & Status Aplikasi") {
                            MonoKeyValue("App Version", "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                            MonoKeyValue("Application ID", BuildConfig.APPLICATION_ID)
                            MonoKeyValue("Android SDK", "API ${Build.VERSION.SDK_INT} (${Build.MODEL})")
                            MonoKeyValue("WebView", summary.webViewVersion)
                            MonoKeyValue("Notif Permission", if (AppNotifications.hasNotificationPermission(context)) "GRANTED" else "DENIED")
                            MonoKeyValue("Akun Google", if (authProfile.isLoggedIn) "Logged in (${authProfile.name})" else "Belum login")
                            MonoKeyValue("Antrean Unduhan", "${downloadJobs.size} total")

                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    val msg = String.format(Locale.US, "PSS %.1f MB | Heap %.1f MB", memory.totalPssMb, memory.jvmUsedMb)
                                    AppNotifications.notifyDebugDiagnostic(context, "YMusic RAM & Debug", msg)
                                    onToast("Notifikasi debug dikirim (ketuk untuk buka DebugActivity)")
                                },
                                modifier = Modifier.fillMaxWidth().testTag("debug_send_notif_btn"),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, YtOrange),
                            ) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = YtOrange, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Kirim Notifikasi Tes Debug", color = YtOrange)
                            }
                        }
                    }
                }

                DebugSectionTab.LIFECYCLE -> {
                    item {
                        DebugInfoCard(title = "Foreground Services Aktif (${summary.runningServices.size})") {
                            if (summary.runningServices.isEmpty()) {
                                Text("Tidak ada service latar belakang aktif.", style = MaterialTheme.typography.bodySmall, color = YtTextSecondary)
                            } else {
                                summary.runningServices.forEach { svc ->
                                    MonoKeyValue("● RUNNING", svc, valueColor = Color(0xFF2E7D32))
                                }
                            }
                        }
                    }
                    item {
                        DebugInfoCard(title = "Riwayat Lifecycle Activity (${summary.activeActivitiesCount} aktif)") {
                            val fmt = remember { SimpleDateFormat("HH:mm:ss", Locale.US) }
                            if (summary.activities.isEmpty()) {
                                Text("Belum ada event activity terekam.", style = MaterialTheme.typography.bodySmall, color = YtTextSecondary)
                            } else {
                                summary.activities.forEach { act ->
                                    val stColor = when (act.state) {
                                        "RESUMED" -> Color(0xFF2E7D32)
                                        "DESTROYED" -> Color(0xFFC62828)
                                        else -> YtOrange
                                    }
                                    val extra = if (act.routeExtra.isNotBlank()) " · route=${act.routeExtra}" else ""
                                    MonoKeyValue(
                                        key = "${act.simpleName} [${fmt.format(Date(act.updatedAt))}]",
                                        value = "${act.state} (${act.intentAction})$extra",
                                        valueColor = stColor,
                                    )
                                }
                            }
                        }
                    }
                }

                DebugSectionTab.AUDIO -> {
                    item {
                        DebugInfoCard(title = "ExoPlayer & 8D Spatial DSP") {
                            MonoKeyValue("Player Mode", playerState.mode.name)
                            MonoKeyValue("Playing", playerState.playing.toString())
                            MonoKeyValue("Resolving Stream", playerState.resolvingStream.toString())
                            MonoKeyValue("Track", playerState.track?.title ?: "-")
                            MonoKeyValue("Codec / Label", playerState.audioCodecLabel.ifBlank { "-" })
                            MonoKeyValue("DSP Enabled", mixerSettings.enabled.toString())
                            MonoKeyValue("Orbit Path", "${mixerSettings.pathId} (${mixerSettings.orbitSpeedHz} Hz)")
                            MonoKeyValue(
                                "Live Pan (X, Z)",
                                String.format(Locale.US, "%.2f, %.2f", panFrame.x, panFrame.z),
                            )
                            MonoKeyValue("Width / Room", "${mixerSettings.width}x / ${mixerSettings.room}x")
                        }
                    }
                }

                DebugSectionTab.LOGS -> {
                    item {
                        Column(
                            modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedTextField(
                                value = logSearch,
                                onValueChange = { logSearch = it },
                                placeholder = { Text("Filter log tag / pesan…", color = YtTextMuted) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = YtOrange,
                                    unfocusedBorderColor = YtDivider,
                                    focusedContainerColor = YtCard,
                                    unfocusedContainerColor = YtCard,
                                    focusedTextColor = YtTextPrimary,
                                    unfocusedTextColor = YtTextPrimary,
                                ),
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                FilterChip(
                                    selected = levelFilter == null,
                                    onClick = { levelFilter = null },
                                    label = { Text("ALL") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = YtOrangeSoft,
                                        selectedLabelColor = YtOrange,
                                    ),
                                )
                                DebugLogLevel.entries.forEach { lvl ->
                                    FilterChip(
                                        selected = levelFilter == lvl,
                                        onClick = { levelFilter = if (levelFilter == lvl) null else lvl },
                                        label = { Text(lvl.label) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = YtOrangeSoft,
                                            selectedLabelColor = YtOrange,
                                        ),
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        val report = AppDiagnostics.buildTextReport()
                                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        cm.setPrimaryClip(ClipData.newPlainText("YMusic Debug Report", report))
                                        onToast("Laporan debug disalin ke clipboard")
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, YtDivider),
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = YtTextPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Salin Laporan", color = YtTextPrimary, maxLines = 1)
                                }
                                OutlinedButton(
                                    onClick = { AppDiagnostics.clearLogs() },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, YtDivider),
                                ) {
                                    Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = YtTextPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Bersihkan", color = YtTextPrimary, maxLines = 1)
                                }
                            }
                        }
                    }

                    val filtered = logs.filter { entry ->
                        (levelFilter == null || entry.level == levelFilter) &&
                            (logSearch.isBlank() || entry.tag.contains(logSearch, true) || entry.message.contains(logSearch, true))
                    }
                    items(filtered, key = { it.id }) { entry ->
                        val badgeColor = when (entry.level) {
                            DebugLogLevel.INFO -> Color(0xFF1E88E5)
                            DebugLogLevel.WARN -> Color(0xFFE6681B)
                            DebugLogLevel.ERROR -> Color(0xFFD32F2F)
                        }
                        val time = remember(entry.timestampMs) {
                            SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(entry.timestampMs))
                        }
                        Row(
                            modifier = Modifier
                                .widthIn(max = 720.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(YtCard)
                                .border(1.dp, YtDivider, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Text(
                                text = "$time ${entry.level.label}",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                                color = badgeColor,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "[${entry.tag}] ${entry.message}",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.5.sp),
                                color = YtTextPrimary,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricProgressRow(
    label: String,
    valueText: String,
    progress: Float,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = YtTextPrimary, fontWeight = FontWeight.SemiBold)
            Text(
                valueText,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = YtTextSecondary,
            )
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = if (progress > 0.85f) Color(0xFFD32F2F) else YtOrange,
            trackColor = YtDivider,
        )
    }
}

@Composable
private fun DebugInfoCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = YtCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, YtDivider),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = YtTextPrimary, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
private fun MonoKeyValue(
    key: String,
    value: String,
    valueColor: Color = YtTextPrimary,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = key,
            style = MaterialTheme.typography.bodySmall,
            color = YtTextSecondary,
            modifier = Modifier.weight(0.45f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = valueColor,
            modifier = Modifier.weight(0.55f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
