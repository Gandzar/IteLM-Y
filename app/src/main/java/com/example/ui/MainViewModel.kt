package com.example.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppRepository
import com.example.data.AppSettings
import com.example.data.AppTab
import com.example.data.DownloadFormat
import com.example.data.DownloadJob
import com.example.data.EngineMeta
import com.example.data.MediaItem
import com.example.data.ReleaseInfo
import com.example.data.ScrapeResult
import com.example.data.ScraprMeta
import com.example.data.ScraprUpdateInfo
import com.example.data.Track
import com.example.download.DownloadManager
import com.example.engine.MediaScraper
import com.example.engine.MusicSearchEngine
import com.example.player.PlayerState
import com.example.player.YMusicPlayer
import dev.bagja.spatialaudio.audio.SpatialHostBridge
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    val repository = AppRepository(appContext)
    val player = YMusicPlayer(appContext)
    val downloadManager = DownloadManager(appContext, repository)

    private val _currentTab = MutableStateFlow(AppTab.DOWNLOAD)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    private val _urlInput = MutableStateFlow("")
    val urlInput: StateFlow<String> = _urlInput.asStateFlow()

    private val _scrapeBusy = MutableStateFlow(false)
    val scrapeBusy: StateFlow<Boolean> = _scrapeBusy.asStateFlow()

    private val _lastScrape = MutableStateFlow<ScrapeResult?>(null)
    val lastScrape: StateFlow<ScrapeResult?> = _lastScrape.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Track>>(emptyList())
    val searchResults: StateFlow<List<Track>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _isDemoSearch = MutableStateFlow(false)
    val isDemoSearch: StateFlow<Boolean> = _isDemoSearch.asStateFlow()

    val library: StateFlow<List<Track>> = repository.libraryFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val history: StateFlow<List<ScrapeResult>> = repository.historyFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val downloadJobs: StateFlow<List<DownloadJob>> = downloadManager.jobs

    val settings: StateFlow<AppSettings> = repository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val scraprMeta: StateFlow<ScraprMeta?> = repository.scraprMetaFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val engineMeta: StateFlow<EngineMeta?> = repository.engineMetaFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val playerState: StateFlow<PlayerState> = player.state

    private val _updateInfo = MutableStateFlow<ReleaseInfo?>(null)
    val updateInfo: StateFlow<ReleaseInfo?> = _updateInfo.asStateFlow()

    private val _updateChecking = MutableStateFlow(false)
    val updateChecking: StateFlow<Boolean> = _updateChecking.asStateFlow()

    private val _scraprInfo = MutableStateFlow<ScraprUpdateInfo?>(null)
    val scraprInfo: StateFlow<ScraprUpdateInfo?> = _scraprInfo.asStateFlow()

    private val _scraprBusy = MutableStateFlow(false)
    val scraprBusy: StateFlow<Boolean> = _scraprBusy.asStateFlow()

    private val _scraprMsg = MutableStateFlow("")
    val scraprMsg: StateFlow<String> = _scraprMsg.asStateFlow()

    private val _engineBusy = MutableStateFlow(false)
    val engineBusy: StateFlow<Boolean> = _engineBusy.asStateFlow()

    private val _engineMsg = MutableStateFlow("")
    val engineMsg: StateFlow<String> = _engineMsg.asStateFlow()

    private val _sheetTrack = MutableStateFlow<Track?>(null)
    val sheetTrack: StateFlow<Track?> = _sheetTrack.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    private var searchJob: Job? = null

    init {
        SpatialHostBridge.onSharedText = { text ->
            handleSharedText(text)
        }
        viewModelScope.launch {
            val s = repository.settingsFlow.first()
            if (s.autoCheckAppUpdate) {
                val info = repository.checkAppUpdate()
                if (info?.isNewer == true) {
                    _updateInfo.value = info
                    emitToast("Update v${info.tag} tersedia — buka Settings")
                }
            }
        }
    }

    fun selectTab(tab: AppTab) {
        _currentTab.value = tab
        if (tab == AppTab.SETTINGS && settings.value.autoUpdateEngine && _scraprInfo.value == null) {
            viewModelScope.launch {
                runCatching {
                    val current = scraprMeta.value?.version ?: AppRepository.BUILTIN_VERSION
                    val info = repository.checkScraprUpdate(current)
                    _scraprInfo.value = info
                    if (info.isNewer) emitToast("Scraper update v${info.latest} tersedia")
                }
            }
        }
    }

    fun updateUrlInput(url: String) {
        _urlInput.value = url
    }

    fun handleSharedText(text: String) {
        val extracted = Regex("https?://\\S+").find(text)?.value ?: text.trim()
        if (extracted.isNotBlank()) {
            _urlInput.value = extracted
            _currentTab.value = AppTab.DOWNLOAD
            scrapeCurrentUrl()
        }
    }

    fun scrapeCurrentUrl() {
        val url = _urlInput.value.trim()
        if (url.isEmpty()) {
            emitToast("Tempel URL dulu")
            return
        }
        if (_scrapeBusy.value) return
        viewModelScope.launch {
            _scrapeBusy.value = true
            try {
                val result = MediaScraper.scrapeUrl(url)
                _lastScrape.value = result
                if (result.status) {
                    val updated = (listOf(result) + history.value).take(50)
                    repository.saveHistory(updated)
                }
                emitToast(if (result.status) "Berhasil" else (result.error ?: "Gagal"))
            } catch (e: Exception) {
                emitToast(e.message ?: "Error")
            } finally {
                _scrapeBusy.value = false
            }
        }
    }

    fun openHistoryItem(item: ScrapeResult) {
        _lastScrape.value = item
        _urlInput.value = item.rawUrl
        _currentTab.value = AppTab.DOWNLOAD
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.saveHistory(emptyList())
            emitToast("Riwayat dibersihkan")
        }
    }

    fun downloadScrapedItem(item: MediaItem) {
        val scrape = _lastScrape.value ?: return
        val track = MediaScraper.scrapeToTrack(scrape)
        if (track != null) {
            val withItemUrl = track.copy(url = item.url)
            val format = if (item.type == "audio" || item.ext == "mp3") DownloadFormat.MP3 else DownloadFormat.MP4
            downloadManager.enqueue(withItemUrl, format)
            if (MediaScraper.isDirectMediaUrl(item.url)) {
                emitToast("Download ${format.label} dimulai")
            } else {
                openExternalUrl(item.url)
            }
        } else {
            openExternalUrl(item.url)
        }
    }

    fun saveLastScrapeToLibrary() {
        val scrape = _lastScrape.value ?: return
        val track = MediaScraper.scrapeToTrack(scrape) ?: return
        addToLibrary(track)
    }

    fun playLastScrape() {
        val scrape = _lastScrape.value ?: return
        val track = MediaScraper.scrapeToTrack(scrape) ?: return
        playTrack(track)
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        val trimmed = query.trim()
        if (trimmed.length < 2) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }
        searchJob = viewModelScope.launch {
            delay(450)
            _isSearching.value = true
            val res = MusicSearchEngine.searchTracks(trimmed, settings.value.youtubeApiKey)
            _searchResults.value = res.tracks
            _isDemoSearch.value = res.isDemo
            _isSearching.value = false
        }
    }

    fun playTrack(track: Track) {
        player.play(track)
        emitToast("Memutar: ${track.title}")
    }

    fun openTrackSheet(track: Track) {
        _sheetTrack.value = track
    }

    fun closeTrackSheet() {
        _sheetTrack.value = null
    }

    fun isInLibrary(trackId: String): Boolean = library.value.any { it.id == trackId }

    fun addToLibrary(track: Track) {
        viewModelScope.launch {
            if (!library.value.any { it.id == track.id }) {
                repository.saveLibrary(listOf(track) + library.value)
            }
            emitToast("Ditambah ke Library")
        }
    }

    fun toggleLibrary(track: Track) {
        viewModelScope.launch {
            val current = library.value
            val exists = current.any { it.id == track.id }
            val next = if (exists) current.filterNot { it.id == track.id } else listOf(track) + current
            repository.saveLibrary(next)
            emitToast(if (exists) "Dihapus dari Library" else "Ditambah ke Library")
        }
    }

    fun enqueueDownload(track: Track, format: DownloadFormat) {
        downloadManager.enqueue(track, format)
        emitToast("Download ${format.label} dimulai")
    }

    fun removeDownloadJob(id: String) = downloadManager.remove(id)
    fun clearFinishedDownloads() = downloadManager.clearFinished()

    fun setAutoCheckAppUpdate(enabled: Boolean) {
        viewModelScope.launch { repository.updateSettings { it.copy(autoCheckAppUpdate = enabled) } }
    }

    fun setAutoUpdateEngine(enabled: Boolean) {
        viewModelScope.launch { repository.updateSettings { it.copy(autoUpdateEngine = enabled) } }
    }

    fun setDownloadFolder(folder: String) {
        val cleaned = folder.trim().ifBlank { "YMusicLite" }
        viewModelScope.launch { repository.updateSettings { it.copy(downloadFolder = cleaned) } }
    }

    fun setCustomEngineUrl(url: String) {
        viewModelScope.launch { repository.updateSettings { it.copy(customEngineUrl = url) } }
    }

    fun setYoutubeApiKey(key: String) {
        viewModelScope.launch { repository.updateSettings { it.copy(youtubeApiKey = key.trim()) } }
    }

    fun checkAppUpdate() {
        if (_updateChecking.value) return
        viewModelScope.launch {
            _updateChecking.value = true
            val info = repository.checkAppUpdate()
            _updateInfo.value = info
            _updateChecking.value = false
            when {
                info == null -> emitToast("Gagal cek update (repo belum ada / offline)")
                info.isNewer -> emitToast("Update v${info.tag} tersedia")
                else -> emitToast("Sudah versi terbaru")
            }
        }
    }

    fun checkScraprUpdate() {
        if (_scraprBusy.value) return
        viewModelScope.launch {
            _scraprBusy.value = true
            _scraprMsg.value = ""
            try {
                val current = scraprMeta.value?.version ?: AppRepository.BUILTIN_VERSION
                val info = repository.checkScraprUpdate(current)
                _scraprInfo.value = info
                val msg = if (info.isNewer) "Update tersedia: v${info.latest}" else "Sudah terbaru: v${info.latest}"
                _scraprMsg.value = msg
                emitToast(msg)
            } catch (e: Exception) {
                val msg = "Error: ${e.message ?: "gagal cek"}"
                _scraprMsg.value = msg
                emitToast(msg)
            } finally {
                _scraprBusy.value = false
            }
        }
    }

    fun updateScraprFromOfficial() {
        if (_scraprBusy.value) return
        viewModelScope.launch {
            _scraprBusy.value = true
            _scraprMsg.value = "Mengunduh metadata dari npm / GitHub…"
            try {
                val meta = repository.updateScraprMetadata()
                runCatching { _scraprInfo.value = repository.checkScraprUpdate(meta.version) }
                _scraprMsg.value = "Berhasil update ke v${meta.version} (${meta.source})"
                emitToast("Scraper v${meta.version} terpasang")
            } catch (e: Exception) {
                val msg = "Error: ${e.message ?: "gagal update"}"
                _scraprMsg.value = msg
                emitToast(msg)
            } finally {
                _scraprBusy.value = false
            }
        }
    }

    fun revertScraprToBuiltin() {
        viewModelScope.launch {
            repository.revertScraprToBuiltin()
            _scraprInfo.value = null
            _scraprMsg.value = "Kembali ke engine bawaan"
            emitToast("Scraper dikembalikan ke bawaan")
        }
    }

    fun downloadCustomEngineConfig(url: String) {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) {
            _engineMsg.value = "Error: isi URL dulu"
            return
        }
        if (_engineBusy.value) return
        viewModelScope.launch {
            _engineBusy.value = true
            _engineMsg.value = ""
            try {
                val meta = repository.fetchCustomEngineConfig(trimmed)
                _engineMsg.value = "Berhasil! Engine v${meta.version}"
                emitToast("Konfigurasi engine diunduh")
            } catch (e: Exception) {
                _engineMsg.value = "Error: ${e.message ?: "gagal"}"
            } finally {
                _engineBusy.value = false
            }
        }
    }

    fun revertCustomEngine() {
        viewModelScope.launch {
            repository.revertCustomEngine()
            _engineMsg.value = "Kembali ke engine bawaan"
            emitToast("Engine bawaan")
        }
    }

    fun openExternalUrl(url: String) {
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            appContext.startActivity(intent)
        }.onFailure {
            emitToast("Tidak bisa membuka tautan")
        }
    }

    fun emitToast(msg: String) {
        _toastMessage.tryEmit(msg)
    }

    override fun onCleared() {
        super.onCleared()
        SpatialHostBridge.onSharedText = null
        player.release()
    }
}
