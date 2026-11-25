package com.example.ui

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.bagja.spatialaudio.audio.FloatingBubbleService
import dev.bagja.spatialaudio.audio.SystemGlobalEqManager
import dev.bagja.spatialaudio.audio.Universal8DService
import dev.bagja.spatialaudio.data.BUILTIN_PRESETS
import dev.bagja.spatialaudio.data.EQ_PRESETS
import dev.bagja.spatialaudio.data.MixerRepository
import dev.bagja.spatialaudio.data.MixerSettings
import dev.bagja.spatialaudio.data.Movement
import dev.bagja.spatialaudio.data.PanFrame
import dev.bagja.spatialaudio.data.Pt
import dev.bagja.spatialaudio.data.ROOM_PRESETS
import dev.bagja.spatialaudio.data.SavedPreset
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class SpatialSubTab(val label: String) {
    SPATIAL("8D Spatial"),
    EFFECTS("Efek"),
    PRESETS("Preset"),
    FLOATING("Melayang"),
}

class SpatialViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext

    val settings: StateFlow<MixerSettings> = MixerRepository.settings
    val frame: StateFlow<PanFrame> = MixerRepository.frame
    val universalCaptureActive: StateFlow<Boolean> = MixerRepository.universalCaptureActive
    val globalEqActive: StateFlow<Boolean> = MixerRepository.globalEqActive
    val floatingBubbleEnabled: StateFlow<Boolean> = MixerRepository.floatingBubbleEnabled
    val floatingLyricsEnabled: StateFlow<Boolean> = MixerRepository.floatingLyricsEnabled
    val globalEqMessage: StateFlow<String?> = SystemGlobalEqManager.statusMessage

    private val _subTab = MutableStateFlow(SpatialSubTab.SPATIAL)
    val subTab: StateFlow<SpatialSubTab> = _subTab.asStateFlow()

    private val _savedPresets = MutableStateFlow<List<SavedPreset>>(emptyList())
    val savedPresets: StateFlow<List<SavedPreset>> = _savedPresets.asStateFlow()

    private val _drawPathMode = MutableStateFlow(false)
    val drawPathMode: StateFlow<Boolean> = _drawPathMode.asStateFlow()

    private val _draftPath = MutableStateFlow<List<Pt>>(emptyList())
    val draftPath: StateFlow<List<Pt>> = _draftPath.asStateFlow()

    private val _presetNameInput = MutableStateFlow("")
    val presetNameInput: StateFlow<String> = _presetNameInput.asStateFlow()

    private val _previewPhase = MutableStateFlow(0f)
    val previewPhase: StateFlow<Float> = _previewPhase.asStateFlow()

    private val _floatingNotice = MutableStateFlow("Kontrol overlay berjalan melalui Android native service.")
    val floatingNotice: StateFlow<String> = _floatingNotice.asStateFlow()

    private val _toast = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val toast: SharedFlow<String> = _toast.asSharedFlow()

    init {
        MixerRepository.init(appContext)
        _savedPresets.value = MixerRepository.loadPresets()
        viewModelScope.launch {
            var lastTick = System.currentTimeMillis()
            while (isActive) {
                delay(50)
                val now = System.currentTimeMillis()
                val dt = (now - lastTick) / 1000f
                lastTick = now
                val s = MixerRepository.settings.value
                if (s.enabled) {
                    val period = s.rotationSeconds.coerceAtLeast(0.66f)
                    val dir = if (s.clockwise) 1f else -1f
                    _previewPhase.value = ((_previewPhase.value + dir * dt / period) % 1f + 1f) % 1f
                }
            }
        }
    }

    fun selectSubTab(tab: SpatialSubTab) {
        _subTab.value = tab
    }

    fun toggleMasterDsp() {
        val next = !settings.value.enabled
        updateSettings { it.copy(enabled = next) }
        _toast.tryEmit(if (next) "DSP diaktifkan" else "DSP dimatikan")
    }

    fun updateSettings(change: (MixerSettings) -> MixerSettings) {
        MixerRepository.update(change)
        if (MixerRepository.globalEqActive.value) {
            SystemGlobalEqManager.apply(MixerRepository.settings.value)
        }
    }

    fun selectPath(pathId: String) {
        _drawPathMode.value = false
        _draftPath.value = emptyList()
        updateSettings { it.copy(pathId = pathId) }
    }

    fun toggleDrawPathMode() {
        val next = !_drawPathMode.value
        _drawPathMode.value = next
        _draftPath.value = emptyList()
    }

    fun appendDraftPoint(pt: Pt) {
        if (!_drawPathMode.value) return
        val cur = _draftPath.value
        val last = cur.lastOrNull()
        if (last == null || kotlin.math.hypot((pt.x - last.x).toDouble(), (pt.z - last.z).toDouble()) > 0.03) {
            _draftPath.value = cur + pt
        }
    }

    fun finishDrawingPath() {
        if (!_drawPathMode.value) return
        val pts = _draftPath.value
        if (pts.size >= 4) {
            updateSettings { it.copy(customRaw = pts, pathId = "custom") }
            _toast.tryEmit("Jalur 8D khusus diterapkan")
        }
        _drawPathMode.value = false
        _draftPath.value = emptyList()
    }

    fun clearCustomPath() {
        _drawPathMode.value = false
        _draftPath.value = emptyList()
        updateSettings {
            it.copy(
                customRaw = emptyList(),
                pathId = if (it.pathId == "custom") "lingkaran" else it.pathId,
            )
        }
        _toast.tryEmit("Jalur gambar dihapus")
    }

    fun resetSpatial() {
        val defaults = MixerSettings()
        _drawPathMode.value = false
        _draftPath.value = emptyList()
        updateSettings {
            it.copy(
                pathId = defaults.pathId,
                movement = defaults.movement,
                clockwise = defaults.clockwise,
                intervalMs = defaults.intervalMs,
                rotationSeconds = defaults.rotationSeconds,
                orbitSpeedHz = defaults.orbitSpeedHz,
                smoothLevel = defaults.smoothLevel,
                lissajousFreqX = defaults.lissajousFreqX,
                lissajousFreqZ = defaults.lissajousFreqZ,
                lissajousPhase = defaults.lissajousPhase,
                customRaw = emptyList(),
                width = defaults.width,
                room = defaults.room,
                offsetX = defaults.offsetX,
                offsetZ = defaults.offsetZ,
                transitionSeconds = defaults.transitionSeconds,
            )
        }
        _toast.tryEmit("Pengaturan ruang direset")
    }

    fun applyEqPreset(label: String) {
        val preset = EQ_PRESETS.firstOrNull { it.label == label } ?: return
        updateSettings { it.copy(eq = preset.gains) }
        _toast.tryEmit("EQ ${preset.label} diterapkan")
    }

    fun setEqBand(index: Int, value: Float) {
        updateSettings { s ->
            val next = s.eq.toMutableList()
            if (index in next.indices) next[index] = value.coerceIn(-15f, 15f)
            s.copy(eq = next)
        }
    }

    fun selectRoomPreset(roomId: String) {
        val room = ROOM_PRESETS.firstOrNull { it.id == roomId } ?: return
        updateSettings { it.copy(roomPresetId = room.id, reverbDecay = room.decay, reverbMix = room.mix) }
    }

    fun applyBuiltinPreset(id: String) {
        val preset = BUILTIN_PRESETS.firstOrNull { it.id == id } ?: return
        MixerRepository.set(preset.build())
        if (MixerRepository.globalEqActive.value) {
            SystemGlobalEqManager.apply(MixerRepository.settings.value)
        }
        _toast.tryEmit("Preset ${preset.name} aktif")
    }

    fun updatePresetNameInput(name: String) {
        _presetNameInput.value = name
    }

    fun saveCurrentPreset() {
        val name = _presetNameInput.value.trim()
        if (name.isEmpty()) {
            _toast.tryEmit("Isi nama preset terlebih dahulu")
            return
        }
        MixerRepository.savePreset(name, settings.value)
        _savedPresets.value = MixerRepository.loadPresets()
        _presetNameInput.value = ""
        _toast.tryEmit("Preset $name tersimpan")
    }

    fun loadSavedPreset(preset: SavedPreset) {
        MixerRepository.set(preset.settings)
        if (MixerRepository.globalEqActive.value) {
            SystemGlobalEqManager.apply(preset.settings)
        }
        _toast.tryEmit("${preset.name} dimuat")
    }

    fun deleteSavedPreset(id: String) {
        MixerRepository.deletePreset(id)
        _savedPresets.value = MixerRepository.loadPresets()
        _toast.tryEmit("Preset dihapus")
    }

    fun exportPresetJson(): String {
        val name = _presetNameInput.value.trim().ifBlank { "Preset Kruzen" }
        return MixerRepository.exportCurrentPresetJson(name)
    }

    fun importPresetFromUri(uri: Uri) {
        runCatching {
            val raw = appContext.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: error("File tidak dapat dibaca")
            MixerRepository.importPresetJson(raw).getOrThrow()
        }.onSuccess { imported ->
            _savedPresets.value = MixerRepository.loadPresets()
            if (MixerRepository.globalEqActive.value) {
                SystemGlobalEqManager.apply(MixerRepository.settings.value)
            }
            _toast.tryEmit("Preset ${imported.name} diimpor")
        }.onFailure { err ->
            _toast.tryEmit("Gagal impor JSON: ${err.message}")
        }
    }

    fun onImpulseResponsePicked(uri: Uri) {
        runCatching {
            appContext.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        val name = runCatching {
            appContext.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        }.getOrNull() ?: "Impulse Response.wav"

        updateSettings { it.copy(customIrUri = uri.toString(), customIrName = name) }
        _toast.tryEmit("Impulse Response tersambung ke engine native")
    }

    fun toggleFloatingBubble(enabled: Boolean) {
        if (enabled && !Settings.canDrawOverlays(appContext)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${appContext.packageName}"),
            ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            appContext.startActivity(intent)
            _floatingNotice.value = "Izin overlay dibuka. Setelah diizinkan, aktifkan kembali toggle ini."
            _toast.tryEmit("Berikan izin overlay di pengaturan Android")
            return
        }
        MixerRepository.floatingBubbleEnabled.value = enabled
        if (enabled) FloatingBubbleService.start(appContext)
        else if (!MixerRepository.floatingLyricsEnabled.value) FloatingBubbleService.stop(appContext)
        else FloatingBubbleService.start(appContext)
        _toast.tryEmit("Status gelembung diperbarui")
    }

    fun toggleFloatingLyrics(enabled: Boolean) {
        if (enabled && !Settings.canDrawOverlays(appContext)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${appContext.packageName}"),
            ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            appContext.startActivity(intent)
            _floatingNotice.value = "Izin overlay dibuka. Setelah diizinkan, aktifkan kembali toggle ini."
            _toast.tryEmit("Berikan izin overlay di pengaturan Android")
            return
        }
        MixerRepository.floatingLyricsEnabled.value = enabled
        if (enabled || MixerRepository.floatingBubbleEnabled.value) {
            FloatingBubbleService.start(appContext)
        } else {
            FloatingBubbleService.stop(appContext)
        }
        FloatingBubbleService.syncLyrics(appContext)
    }

    fun toggleGlobalEq(enabled: Boolean) {
        SystemGlobalEqManager.setEnabled(enabled, settings.value)
    }

    fun startUniversalCapture(resultCode: Int, data: Intent?) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            _toast.tryEmit("Universal Capture membutuhkan Android 10+")
            return
        }
        if (resultCode != Activity.RESULT_OK || data == null) {
            _toast.tryEmit("Persetujuan capture dibatalkan")
            return
        }
        Universal8DService.start(appContext, resultCode, data)
        _toast.tryEmit("Universal 8D Capture dimulai")
    }

    fun stopUniversalCapture() {
        Universal8DService.stop(appContext)
        _toast.tryEmit("Universal 8D Capture dimatikan")
    }

    fun setMovement(m: Movement) = updateSettings { it.copy(movement = m) }
}
