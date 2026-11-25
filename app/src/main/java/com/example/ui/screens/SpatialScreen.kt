package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.SpatialSubTab
import com.example.ui.SpatialViewModel
import com.example.ui.theme.SpatialBg
import com.example.ui.theme.SpatialBorder
import com.example.ui.theme.SpatialCyan
import com.example.ui.theme.SpatialGreen
import com.example.ui.theme.SpatialGreenDeep
import com.example.ui.theme.SpatialMuted
import com.example.ui.theme.SpatialPanel
import com.example.ui.theme.SpatialPanelAlt
import com.example.ui.theme.TextPrimary
import dev.bagja.spatialaudio.data.MixerSettings
import dev.bagja.spatialaudio.data.Movement
import dev.bagja.spatialaudio.data.PanFrame
import dev.bagja.spatialaudio.data.Pt
import dev.bagja.spatialaudio.data.SavedPreset
import dev.bagja.spatialaudio.dsp.PathLib
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private data class PathOption(val id: String, val label: String, val hint: String)

private val ALL_PATHS = PathLib.builtin.map { PathOption(it.id, it.label, it.hint) } +
    PathOption(PathLib.LISSAJOUS, "Lissajous", "Kurva dua frekuensi yang dapat diatur")

@Composable
fun SpatialScreen(
    viewModel: SpatialViewModel,
    subTab: SpatialSubTab,
    settings: MixerSettings,
    frame: PanFrame,
    previewPhase: Float,
    drawPathMode: Boolean,
    draftPath: List<Pt>,
    savedPresets: List<SavedPreset>,
    presetNameInput: String,
    universalCaptureActive: Boolean,
    globalEqActive: Boolean,
    globalEqMessage: String?,
    floatingBubbleEnabled: Boolean,
    floatingLyricsEnabled: Boolean,
    floatingNotice: String,
    modifier: Modifier = Modifier,
) {
    CompositionLocalProvider(LocalContentColor provides TextPrimary) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SpatialBg),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Studio Topbar + Hero
        item {
            Column(modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Brush.linearGradient(listOf(SpatialGreen, SpatialGreenDeep))),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("K◉", color = Color(0xFF06150F), fontWeight = FontWeight.Black)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("KRUZEN SPATIAL", style = MaterialTheme.typography.labelMedium, color = SpatialGreen)
                            Text("Audio Control Studio", style = MaterialTheme.typography.bodySmall, color = SpatialMuted)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(SpatialGreen),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("ANDROID NATIVE", style = MaterialTheme.typography.labelSmall, color = SpatialMuted)
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = SpatialPanel),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF254B44)),
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Image(
                            painter = painterResource(id = R.drawable.img_spatial_hero),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .matchParentSize()
                                .alpha(0.32f),
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(SpatialPanel.copy(alpha = 0.94f), SpatialPanelAlt.copy(alpha = 0.72f)),
                                    ),
                                ),
                        )
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text("KRUZENLAB / BAGJA.DEV", style = MaterialTheme.typography.labelSmall, color = SpatialGreen)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Spatial sound. Beyond stereo.",
                                style = MaterialTheme.typography.headlineSmall,
                                color = TextPrimary,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Atur pergerakan 8D, karakter audio, dan preset dalam satu studio.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SpatialMuted,
                            )
                            Spacer(Modifier.height(14.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = { viewModel.toggleMasterDsp() },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (settings.enabled) SpatialGreen else SpatialPanelAlt,
                                        contentColor = if (settings.enabled) Color(0xFF061710) else SpatialMuted,
                                    ),
                                    modifier = Modifier.testTag("spatial_master_toggle"),
                                ) {
                                    Text(
                                        text = if (settings.enabled) "⏻ DSP AKTIF" else "⏻ DSP NONAKTIF",
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                                OutlinedButton(
                                    onClick = { viewModel.selectSubTab(SpatialSubTab.PRESETS) },
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, SpatialBorder),
                                ) {
                                    Text("Jelajahi preset ↗", color = TextPrimary)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Sub-tab bar
        item {
            Surface(
                modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = SpatialPanel,
                border = androidx.compose.foundation.BorderStroke(1.dp, SpatialBorder),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    SpatialSubTab.entries.forEach { tab ->
                        val active = tab == subTab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (active) Color(0xFF173B31) else Color.Transparent)
                                .then(
                                    if (active) Modifier.border(1.dp, Color(0xFF32735B), RoundedCornerShape(10.dp))
                                    else Modifier,
                                )
                                .clickable { viewModel.selectSubTab(tab) }
                                .padding(vertical = 10.dp, horizontal = 4.dp)
                                .testTag("spatial_subtab_${tab.name.lowercase()}"),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (active) SpatialGreen else SpatialMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }

        when (subTab) {
            SpatialSubTab.SPATIAL -> {
                item {
                    OrbitAndSpectrumSection(
                        settings = settings,
                        frame = frame,
                        previewPhase = previewPhase,
                        drawPathMode = drawPathMode,
                        draftPath = draftPath,
                        onToggleDraw = viewModel::toggleDrawPathMode,
                        onClearPath = viewModel::clearCustomPath,
                        onAppendPoint = viewModel::appendDraftPoint,
                        onFinishDraw = viewModel::finishDrawingPath,
                        modifier = Modifier.widthIn(max = 720.dp),
                    )
                }
                item {
                    PathSelectorPanel(
                        settings = settings,
                        onSelectPath = viewModel::selectPath,
                        onUpdateSettings = viewModel::updateSettings,
                        onSetMovement = viewModel::setMovement,
                        modifier = Modifier.widthIn(max = 720.dp),
                    )
                }
                item {
                    SpaceControlsPanel(
                        settings = settings,
                        onReset = viewModel::resetSpatial,
                        onUpdateSettings = viewModel::updateSettings,
                        modifier = Modifier.widthIn(max = 720.dp),
                    )
                }
            }

            SpatialSubTab.EFFECTS -> {
                item {
                    SpatialEffectsTabContent(
                        settings = settings,
                        onApplyEqPreset = viewModel::applyEqPreset,
                        onSetEqBand = viewModel::setEqBand,
                        onSelectRoom = viewModel::selectRoomPreset,
                        onUpdateSettings = viewModel::updateSettings,
                        onImpulseResponsePicked = viewModel::onImpulseResponsePicked,
                        modifier = Modifier.widthIn(max = 720.dp),
                    )
                }
            }

            SpatialSubTab.PRESETS -> {
                item {
                    SpatialPresetsTabContent(
                        presetNameInput = presetNameInput,
                        savedPresets = savedPresets,
                        onNameChange = viewModel::updatePresetNameInput,
                        onSavePreset = viewModel::saveCurrentPreset,
                        onApplyBuiltin = viewModel::applyBuiltinPreset,
                        onApplyEqAndSwitch = { label ->
                            viewModel.applyEqPreset(label)
                            viewModel.selectSubTab(SpatialSubTab.EFFECTS)
                        },
                        onLoadSaved = viewModel::loadSavedPreset,
                        onDeleteSaved = viewModel::deleteSavedPreset,
                        onExportJson = viewModel::exportPresetJson,
                        onImportJsonUri = viewModel::importPresetFromUri,
                        modifier = Modifier.widthIn(max = 720.dp),
                    )
                }
            }

            SpatialSubTab.FLOATING -> {
                item {
                    SpatialFloatingTabContent(
                        floatingBubbleEnabled = floatingBubbleEnabled,
                        floatingLyricsEnabled = floatingLyricsEnabled,
                        floatingNotice = floatingNotice,
                        universalCaptureActive = universalCaptureActive,
                        globalEqActive = globalEqActive,
                        globalEqMessage = globalEqMessage,
                        onToggleBubble = viewModel::toggleFloatingBubble,
                        onToggleLyrics = viewModel::toggleFloatingLyrics,
                        onToggleGlobalEq = viewModel::toggleGlobalEq,
                        onStartCapture = viewModel::startUniversalCapture,
                        onStopCapture = viewModel::stopUniversalCapture,
                        modifier = Modifier.widthIn(max = 720.dp),
                    )
                }
            }
        }
    }
    }
}

@Composable
private fun OrbitAndSpectrumSection(
    settings: MixerSettings,
    frame: PanFrame,
    previewPhase: Float,
    drawPathMode: Boolean,
    draftPath: List<Pt>,
    onToggleDraw: () -> Unit,
    onClearPath: () -> Unit,
    onAppendPoint: (Pt) -> Unit,
    onFinishDraw: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pathPoints = remember(
        settings.pathId,
        settings.customRaw,
        settings.smoothLevel,
        settings.lissajousFreqX,
        settings.lissajousFreqZ,
        settings.lissajousPhase,
    ) {
        val arr = PathLib.resolve(
            settings.pathId,
            settings.customRaw,
            settings.smoothLevel,
            settings.lissajousFreqX,
            settings.lissajousFreqZ,
            settings.lissajousPhase,
        )
        List(arr.size / 2) { i -> Pt(arr[i * 2], arr[i * 2 + 1]) }
    }

    val hasLiveAudio = frame.spectrum.any { it > 0.002f }
    val activePhase = if (hasLiveAudio) frame.progress.coerceIn(0f, 1f) else previewPhase
    val activeIdx = ((activePhase * pathPoints.size).toInt()).coerceIn(0, (pathPoints.size - 1).coerceAtLeast(0))
    val currentOrb = if (hasLiveAudio) Pt(frame.x, frame.z) else pathPoints.getOrElse(activeIdx) { Pt(0f, -1f) }
    val pathLabel = if (settings.pathId == PathLib.CUSTOM) {
        "Jalur sendiri"
    } else {
        ALL_PATHS.firstOrNull { it.id == settings.pathId }?.label ?: settings.pathId
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SpatialPanelCard(
            title = "Orbit visualizer",
            subtitle = "Posisi virtual suara mengelilingi pendengar",
            badge = if (settings.enabled) "ENGINE ON" else "ENGINE OFF",
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF0A0F15))
                    .then(
                        if (drawPathMode) {
                            Modifier.border(1.dp, SpatialGreen, RoundedCornerShape(14.dp))
                                .pointerInput(drawPathMode) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            val rx = size.width * 0.34f * 0.86f
                                            val rz = size.height * 0.36f * 0.86f
                                            val nx = ((offset.x - size.width / 2f) / rx).coerceIn(-1f, 1f)
                                            val nz = ((offset.y - size.height / 2f) / rz).coerceIn(-1f, 1f)
                                            onAppendPoint(Pt(nx, nz))
                                        },
                                        onDrag = { change, _ ->
                                            change.consume()
                                            val rx = size.width * 0.34f * 0.86f
                                            val rz = size.height * 0.36f * 0.86f
                                            val nx = ((change.position.x - size.width / 2f) / rx).coerceIn(-1f, 1f)
                                            val nz = ((change.position.y - size.height / 2f) / rz).coerceIn(-1f, 1f)
                                            onAppendPoint(Pt(nx, nz))
                                        },
                                        onDragEnd = { onFinishDraw() },
                                        onDragCancel = { onFinishDraw() },
                                    )
                                }
                        } else Modifier,
                    ),
            ) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val rx = size.width * 0.34f
                val rz = size.height * 0.36f

                for (i in 1..3) {
                    drawOval(
                        color = Color(0xFF263A43),
                        topLeft = Offset(cx - rx * i / 3f, cy - rz * i / 3f),
                        size = Size(rx * 2f * i / 3f, rz * 2f * i / 3f),
                        style = Stroke(width = 1.dp.toPx()),
                    )
                }
                drawLine(Color(0xFF32464E), Offset(cx - rx, cy), Offset(cx + rx, cy), strokeWidth = 1.dp.toPx())
                drawLine(Color(0xFF32464E), Offset(cx, cy - rz), Offset(cx, cy + rz), strokeWidth = 1.dp.toPx())

                if (pathPoints.size > 1) {
                    val p = Path()
                    pathPoints.forEachIndexed { idx, pt ->
                        val px = cx + pt.x * rx * 0.86f
                        val py = cy + pt.z * rz * 0.86f
                        if (idx == 0) p.moveTo(px, py) else p.lineTo(px, py)
                    }
                    p.close()
                    drawPath(p, color = SpatialGreen, style = Stroke(width = 2.5.dp.toPx()))
                }

                if (draftPath.size > 1) {
                    val dp = Path()
                    draftPath.forEachIndexed { idx, pt ->
                        val px = cx + pt.x * rx * 0.86f
                        val py = cy + pt.z * rz * 0.86f
                        if (idx == 0) dp.moveTo(px, py) else dp.lineTo(px, py)
                    }
                    drawPath(
                        dp,
                        color = Color(0xFFF7D58A),
                        style = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)),
                        ),
                    )
                }

                val orbX = cx + currentOrb.x * rx * 0.86f
                val orbY = cy + currentOrb.z * rz * 0.86f
                drawCircle(color = SpatialGreen.copy(alpha = 0.25f), radius = 16.dp.toPx(), center = Offset(orbX, orbY))
                drawCircle(color = Color(0xFFD8FFF0), radius = 7.dp.toPx(), center = Offset(orbX, orbY))
            }

            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("◀ KIRI", style = MaterialTheme.typography.labelSmall, color = SpatialMuted)
                Text("DEPAN", style = MaterialTheme.typography.labelSmall, color = SpatialMuted)
                Text("KANAN ▶", style = MaterialTheme.typography.labelSmall, color = SpatialMuted)
            }

            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = onToggleDraw,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SpatialBorder),
                ) {
                    Text(if (drawPathMode) "Selesai menggambar" else "Gambar jalur sendiri", color = TextPrimary)
                }
                TextButton(onClick = onClearPath) {
                    Text("Hapus jalur", color = SpatialGreen)
                }
            }
            Text(
                text = if (drawPathMode) "Tarik jari pada kanvas untuk membentuk lintasan." else "Pilih “Gambar jalur sendiri” lalu coret pada kanvas.",
                style = MaterialTheme.typography.bodySmall,
                color = SpatialMuted,
            )

            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ReadoutItem("POSISI X", String.format(Locale.US, "%.2f", currentOrb.x))
                ReadoutItem("POSISI Z", String.format(Locale.US, "%.2f", currentOrb.z))
                ReadoutItem("POLA", pathLabel)
            }
        }

        SpatialPanelCard(
            title = "Audio spectrum",
            subtitle = "Analyzer real-time · 24 bands",
            badge = "24 BAND",
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0A0F15)),
            ) {
                for (y in 1..3) {
                    drawLine(
                        color = Color(0xFF1F3239),
                        start = Offset(0f, size.height * y / 4f),
                        end = Offset(size.width, size.height * y / 4f),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
                val vals = if (frame.spectrum.size == 24) frame.spectrum else List(24) { 0f }
                val gap = 4.dp.toPx()
                val bw = ((size.width - gap * 25f) / 24f).coerceAtLeast(2f)
                vals.forEachIndexed { idx, v ->
                    val level = v.coerceIn(0f, 1f)
                    val barH = (level * (size.height - 12.dp.toPx())).coerceAtLeast(3.dp.toPx())
                    val x = gap + idx * (bw + gap)
                    val y = size.height - barH
                    drawRoundRect(
                        brush = Brush.verticalGradient(listOf(Color(0xFF72F5C0), Color(0xFF178C72)), startY = y, endY = size.height),
                        topLeft = Offset(x, y),
                        size = Size(bw, barH),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("LOW", style = MaterialTheme.typography.labelSmall, color = SpatialMuted)
                Text("MID", style = MaterialTheme.typography.labelSmall, color = SpatialMuted)
                Text("HIGH", style = MaterialTheme.typography.labelSmall, color = SpatialMuted)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (hasLiveAudio) "Analyzer real-time · tersambung ke DSP native." else "Menunggu sinyal dari audio engine native.",
                style = MaterialTheme.typography.bodySmall,
                color = SpatialMuted,
            )
        }
    }
}

@Composable
private fun ReadoutItem(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = SpatialMuted)
        Text(value, style = MaterialTheme.typography.labelMedium, color = SpatialGreen)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PathSelectorPanel(
    settings: MixerSettings,
    onSelectPath: (String) -> Unit,
    onUpdateSettings: ((MixerSettings) -> MixerSettings) -> Unit,
    onSetMovement: (Movement) -> Unit,
    modifier: Modifier = Modifier,
) {
    SpatialPanelCard(
        title = "Pilih lintasan",
        subtitle = "Pilih cara suara bergerak di ruang virtual",
        modifier = modifier,
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = 2,
        ) {
            ALL_PATHS.forEach { p ->
                val active = settings.pathId == p.id
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.48f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (active) Color(0xFF15362D) else SpatialPanelAlt)
                        .border(
                            1.dp,
                            if (active) SpatialGreen else SpatialBorder,
                            RoundedCornerShape(12.dp),
                        )
                        .clickable { onSelectPath(p.id) }
                        .padding(10.dp),
                ) {
                    Text(
                        text = p.label,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (active) Color(0xFF86F2C1) else TextPrimary,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = p.hint,
                        style = MaterialTheme.typography.bodySmall,
                        color = SpatialMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        SpatialSlider(
            label = "Kecepatan orbit",
            value = settings.orbitSpeedHz,
            range = 0.02f..1.5f,
            formatted = String.format(Locale.US, "%.2f Hz", settings.orbitSpeedHz),
            onValueChange = { hz ->
                onUpdateSettings {
                    it.copy(orbitSpeedHz = hz, rotationSeconds = (1f / hz.coerceAtLeast(0.02f)).coerceIn(0.66f, 60f))
                }
            },
        )
        SpatialSlider(
            label = "Durasi per putaran",
            value = settings.rotationSeconds,
            range = 0.66f..60f,
            formatted = String.format(Locale.US, "%.1f dtk", settings.rotationSeconds),
            onValueChange = { sec ->
                onUpdateSettings {
                    it.copy(rotationSeconds = sec, orbitSpeedHz = (1f / sec.coerceAtLeast(0.66f)).coerceIn(0.02f, 1.5f))
                }
            },
        )
        SpatialSlider(
            label = "Interval ganti arah",
            value = settings.intervalMs,
            range = 100f..10000f,
            formatted = "${settings.intervalMs.roundToInt()} ms",
            hint = "Dipakai untuk mode bolak-balik atau acak",
            onValueChange = { ms -> onUpdateSettings { it.copy(intervalMs = ms) } },
        )
        SpatialSlider(
            label = "Penghalusan lintasan",
            value = settings.smoothLevel.toFloat(),
            range = 0f..8f,
            formatted = "${settings.smoothLevel}",
            hint = "Level 0 mentah · 8 paling halus",
            onValueChange = { lvl -> onUpdateSettings { it.copy(smoothLevel = lvl.roundToInt()) } },
        )

        if (settings.pathId == PathLib.LISSAJOUS) {
            SpatialSlider(
                label = "Frekuensi X",
                value = settings.lissajousFreqX.toFloat(),
                range = 1f..8f,
                formatted = "${settings.lissajousFreqX}",
                onValueChange = { v -> onUpdateSettings { it.copy(lissajousFreqX = v.roundToInt()) } },
            )
            SpatialSlider(
                label = "Frekuensi Z",
                value = settings.lissajousFreqZ.toFloat(),
                range = 1f..8f,
                formatted = "${settings.lissajousFreqZ}",
                onValueChange = { v -> onUpdateSettings { it.copy(lissajousFreqZ = v.roundToInt()) } },
            )
            SpatialSlider(
                label = "Fase Lissajous",
                value = settings.lissajousPhase.toFloat(),
                range = 0f..360f,
                formatted = "${settings.lissajousPhase}°",
                onValueChange = { v -> onUpdateSettings { it.copy(lissajousPhase = v.roundToInt()) } },
            )
        }

        Spacer(Modifier.height(8.dp))
        Text("JENIS PERGERAKAN", style = MaterialTheme.typography.labelSmall, color = SpatialMuted)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Movement.entries.forEach { m ->
                ChoiceChipButton(
                    label = m.label,
                    selected = settings.movement == m,
                    onClick = { onSetMovement(m) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Text("ARAH PUTAR", style = MaterialTheme.typography.labelSmall, color = SpatialMuted)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChoiceChipButton(
                label = "Searah jarum jam",
                selected = settings.clockwise,
                onClick = { onUpdateSettings { it.copy(clockwise = true) } },
                modifier = Modifier.weight(1f),
            )
            ChoiceChipButton(
                label = "Berlawanan arah",
                selected = !settings.clockwise,
                onClick = { onUpdateSettings { it.copy(clockwise = false) } },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SpaceControlsPanel(
    settings: MixerSettings,
    onReset: () -> Unit,
    onUpdateSettings: ((MixerSettings) -> MixerSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    SpatialPanelCard(
        title = "Ruang & posisi",
        subtitle = "Lebar stereo, jarak, dan titik pusat orbit",
        actionLabel = "Reset",
        onAction = onReset,
        modifier = modifier,
    ) {
        SpatialSlider(
            label = "Spatial width",
            value = settings.width,
            range = 0f..2f,
            formatted = String.format(Locale.US, "%.2f×", settings.width),
            hint = "0 = mono · 2 = 200%",
            onValueChange = { v -> onUpdateSettings { it.copy(width = v) } },
        )
        SpatialSlider(
            label = "Room distance",
            value = settings.room,
            range = 0.2f..3f,
            formatted = String.format(Locale.US, "%.1f×", settings.room),
            hint = "Jarak virtual dekat sampai jauh",
            onValueChange = { v -> onUpdateSettings { it.copy(room = v) } },
        )
        SpatialSlider(
            label = "Transisi perpindahan",
            value = settings.transitionSeconds,
            range = 0f..3f,
            formatted = String.format(Locale.US, "%.1f dtk", settings.transitionSeconds),
            onValueChange = { v -> onUpdateSettings { it.copy(transitionSeconds = v) } },
        )
        SpatialSlider(
            label = "Geser kiri ↔ kanan",
            value = settings.offsetX,
            range = -1f..1f,
            formatted = String.format(Locale.US, "%.2f", settings.offsetX),
            onValueChange = { v -> onUpdateSettings { it.copy(offsetX = v) } },
        )
        SpatialSlider(
            label = "Geser depan ↔ belakang",
            value = settings.offsetZ,
            range = -1f..1f,
            formatted = String.format(Locale.US, "%.2f", settings.offsetZ),
            onValueChange = { v -> onUpdateSettings { it.copy(offsetZ = v) } },
        )
    }
}

@Composable
fun SpatialPanelCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    badge: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SpatialPanel),
        border = androidx.compose.foundation.BorderStroke(1.dp, SpatialBorder),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Spacer(Modifier.height(2.dp))
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = SpatialMuted)
                }
                if (badge != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0xFF15372C))
                            .border(1.dp, Color(0xFF327C60), RoundedCornerShape(999.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(badge, style = MaterialTheme.typography.labelSmall, color = SpatialGreen)
                    }
                } else if (actionLabel != null && onAction != null) {
                    TextButton(onClick = onAction) {
                        Text(actionLabel, color = SpatialGreen)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun SpatialSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    formatted: String,
    hint: String? = null,
    onValueChange: (Float) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0C1219))
            .border(1.dp, Color(0xFF202C37), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = Color(0xFFB8C7CE), fontWeight = FontWeight.SemiBold)
            Text(formatted, style = MaterialTheme.typography.labelMedium, color = SpatialGreen)
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = SpatialGreen,
                activeTrackColor = SpatialGreen,
                inactiveTrackColor = SpatialBorder,
            ),
        )
        if (!hint.isNullOrBlank()) {
            Text(hint, style = MaterialTheme.typography.bodySmall, color = SpatialMuted)
        }
    }
}

@Composable
fun ChoiceChipButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(if (selected) Color(0xFF15372E) else SpatialPanelAlt)
            .border(1.dp, if (selected) Color(0xFF3BC894) else SpatialBorder, RoundedCornerShape(9.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Color(0xFF7DF0BC) else SpatialMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
