package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.SpatialBorder
import com.example.ui.theme.SpatialGreen
import com.example.ui.theme.SpatialGreenDeep
import com.example.ui.theme.SpatialMuted
import com.example.ui.theme.TextPrimary
import dev.bagja.spatialaudio.data.EQ_LABELS
import dev.bagja.spatialaudio.data.EQ_PRESETS
import dev.bagja.spatialaudio.data.MixerSettings
import dev.bagja.spatialaudio.data.ROOM_PRESETS
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpatialEffectsTabContent(
    settings: MixerSettings,
    onApplyEqPreset: (String) -> Unit,
    onSetEqBand: (Int, Float) -> Unit,
    onSelectRoom: (String) -> Unit,
    onUpdateSettings: ((MixerSettings) -> MixerSettings) -> Unit,
    onImpulseResponsePicked: (Uri) -> Unit,
    modifier: Modifier = Modifier,
) {
    val irPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) onImpulseResponsePicked(uri)
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 10-band EQ
        SpatialPanelCard(
            title = "10-band equalizer",
            subtitle = "Pengaturan frekuensi dari sub-bass sampai high air",
            actionLabel = "Flat EQ",
            onAction = { onApplyEqPreset("Datar") },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                EQ_PRESETS.forEach { preset ->
                    ChoiceChipButton(
                        label = preset.label,
                        selected = settings.eq == preset.gains,
                        onClick = { onApplyEqPreset(preset.label) },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            EQ_LABELS.forEachIndexed { idx, hzLabel ->
                val gain = settings.eq.getOrElse(idx) { 0f }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "$hzLabel Hz",
                        style = MaterialTheme.typography.labelSmall,
                        color = SpatialMuted,
                        modifier = Modifier.width(48.dp),
                    )
                    Slider(
                        value = gain.coerceIn(-15f, 15f),
                        onValueChange = { onSetEqBand(idx, it) },
                        valueRange = -15f..15f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = SpatialGreen,
                            activeTrackColor = SpatialGreen,
                            inactiveTrackColor = SpatialBorder,
                        ),
                    )
                    Text(
                        text = "${if (gain > 0) "+" else ""}${gain.roundToInt()} dB",
                        style = MaterialTheme.typography.labelSmall,
                        color = SpatialGreen,
                        modifier = Modifier.width(48.dp),
                    )
                }
            }
        }

        // Enhancement
        SpatialPanelCard(
            title = "Enhancement",
            subtitle = "Penguatan bass, kejernihan vokal, dan kontrol kanal",
        ) {
            SpatialSlider(
                label = "Preamp gain",
                value = settings.preampDb,
                range = -12f..12f,
                formatted = String.format(Locale.US, "%+.1f dB", settings.preampDb),
                onValueChange = { v -> onUpdateSettings { it.copy(preampDb = v) } },
            )
            SpatialSlider(
                label = "Bass boost",
                value = settings.bassDb,
                range = 0f..15f,
                formatted = String.format(Locale.US, "+%.1f dB", settings.bassDb),
                onValueChange = { v -> onUpdateSettings { it.copy(bassDb = v) } },
            )
            SpatialSlider(
                label = "Sub-bass punch",
                value = settings.subBassPunch,
                range = 0f..1f,
                formatted = "${(settings.subBassPunch * 100).roundToInt()}%",
                onValueChange = { v -> onUpdateSettings { it.copy(subBassPunch = v) } },
            )
            SpatialSlider(
                label = "Vocal clarity / exciter",
                value = settings.vocalExciter,
                range = 0f..1f,
                formatted = "${(settings.vocalExciter * 100).roundToInt()}%",
                onValueChange = { v -> onUpdateSettings { it.copy(vocalExciter = v) } },
            )
            SpatialSlider(
                label = "Channel balance",
                value = settings.channelBalance,
                range = -1f..1f,
                formatted = String.format(Locale.US, "%.2f", settings.channelBalance),
                hint = "Negatif = kiri · positif = kanan",
                onValueChange = { v -> onUpdateSettings { it.copy(channelBalance = v) } },
            )
            SpatialSlider(
                label = "Volume utama DSP",
                value = settings.volume,
                range = 0f..1f,
                formatted = "${(settings.volume * 100).roundToInt()}%",
                onValueChange = { v -> onUpdateSettings { it.copy(volume = v) } },
            )
        }

        // Filter & Isolator
        SpatialPanelCard(
            title = "Filter & isolator",
            subtitle = "Pisahkan atau bentuk bagian tertentu dari audio",
        ) {
            Text("MODE VOCAL / BASS ISOLATOR", style = MaterialTheme.typography.labelSmall, color = SpatialMuted)
            Spacer(Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf("Mati", "Karaoke", "Acapella", "Bass Only").forEachIndexed { idx, label ->
                    ChoiceChipButton(
                        label = label,
                        selected = settings.vocalIsolatorMode == idx,
                        onClick = { onUpdateSettings { it.copy(vocalIsolatorMode = idx) } },
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            SpatialSlider(
                label = "High-pass filter",
                value = settings.highpassHz,
                range = 20f..1000f,
                formatted = "${settings.highpassHz.roundToInt()} Hz",
                onValueChange = { v -> onUpdateSettings { it.copy(highpassHz = v) } },
            )
            SpatialSlider(
                label = "Low-pass filter",
                value = settings.lowpassHz,
                range = 300f..20000f,
                formatted = "${settings.lowpassHz.roundToInt()} Hz",
                onValueChange = { v -> onUpdateSettings { it.copy(lowpassHz = v) } },
            )
        }

        // Acoustic Space
        SpatialPanelCard(
            title = "Acoustic space",
            subtitle = "Karakter pantulan dan gema ruang",
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                maxItemsInEachRow = 3,
            ) {
                ROOM_PRESETS.forEach { room ->
                    ChoiceChipButton(
                        label = room.label,
                        selected = settings.roomPresetId == room.id,
                        onClick = { onSelectRoom(room.id) },
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            SpatialSlider(
                label = "Dry / wet reverb",
                value = settings.reverbMix,
                range = 0f..1f,
                formatted = "${(settings.reverbMix * 100).roundToInt()}%",
                onValueChange = { v -> onUpdateSettings { it.copy(reverbMix = v) } },
            )
            SpatialSlider(
                label = "Reverb decay",
                value = settings.reverbDecay,
                range = 0.3f..8f,
                formatted = String.format(Locale.US, "%.1f dtk", settings.reverbDecay),
                onValueChange = { v -> onUpdateSettings { it.copy(reverbDecay = v) } },
            )
            SpatialSlider(
                label = "Echo delay",
                value = settings.echoMs,
                range = 50f..1000f,
                formatted = "${settings.echoMs.roundToInt()} ms",
                onValueChange = { v -> onUpdateSettings { it.copy(echoMs = v) } },
            )
            SpatialSlider(
                label = "Echo feedback",
                value = settings.echoFeedback,
                range = 0f..0.85f,
                formatted = "${(settings.echoFeedback * 100).roundToInt()}%",
                onValueChange = { v -> onUpdateSettings { it.copy(echoFeedback = v) } },
            )
            SpatialSlider(
                label = "Echo mix",
                value = settings.echoMix,
                range = 0f..1f,
                formatted = "${(settings.echoMix * 100).roundToInt()}%",
                onValueChange = { v -> onUpdateSettings { it.copy(echoMix = v) } },
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { irPickerLauncher.launch(arrayOf("audio/wav", "audio/x-wav", "audio/*")) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SpatialBorder),
            ) {
                Text("Pilih Impulse Response (.wav)", color = TextPrimary)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = settings.customIrName?.let { "$it · siap dipakai native convolver" } ?: "Belum ada IR khusus",
                style = MaterialTheme.typography.bodySmall,
                color = SpatialMuted,
            )
        }

        // Character & Output
        SpatialPanelCard(
            title = "Karakter & output",
            subtitle = "Tempo, pitch, kompresi, saturasi, dan volume",
        ) {
            SpatialSlider(
                label = "Tempo / kecepatan",
                value = settings.tempo,
                range = 0.5f..2f,
                formatted = String.format(Locale.US, "%.2f×", settings.tempo),
                onValueChange = { v -> onUpdateSettings { it.copy(tempo = v) } },
            )
            SpatialSlider(
                label = "Pitch",
                value = settings.pitchSemitones,
                range = -12f..12f,
                formatted = String.format(Locale.US, "%+.1f semitone", settings.pitchSemitones),
                onValueChange = { v -> onUpdateSettings { it.copy(pitchSemitones = v) } },
            )
            SpatialSlider(
                label = "Drive / saturasi",
                value = settings.drive,
                range = 0f..1f,
                formatted = "${(settings.drive * 100).roundToInt()}%",
                onValueChange = { v -> onUpdateSettings { it.copy(drive = v) } },
            )
            SpatialSlider(
                label = "Dynamic compressor",
                value = settings.compressor,
                range = 0f..1f,
                formatted = "${(settings.compressor * 100).roundToInt()}%",
                onValueChange = { v -> onUpdateSettings { it.copy(compressor = v) } },
            )
            Spacer(Modifier.height(6.dp))
            SpatialSwitchRow(
                title = "Keep pitch",
                subtitle = "Pertahankan pitch saat tempo berubah",
                checked = settings.keepPitch,
                onCheckedChange = { c -> onUpdateSettings { it.copy(keepPitch = c) } },
            )
            SpatialSwitchRow(
                title = "Soft limiter",
                subtitle = "Menahan puncak agar tidak mudah clipping",
                checked = settings.softLimiter,
                onCheckedChange = { c -> onUpdateSettings { it.copy(softLimiter = c) } },
            )
        }
    }
}

@Composable
fun SpatialSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = SpatialMuted)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = SpatialGreenDeep,
                uncheckedThumbColor = TextPrimary.copy(alpha = 0.85f),
                uncheckedTrackColor = SpatialBorder,
            ),
        )
    }
}
