package com.example.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.auth.YouTubeAuthManager
import com.example.data.AccountProfile
import com.example.data.AppRepository
import com.example.data.AppSettings
import com.example.data.AppTab
import com.example.data.DownloadFormat
import com.example.data.DownloadJob
import com.example.data.EngineMeta
import com.example.data.HistorySection
import com.example.data.HomeChip
import com.example.data.LibrarySection
import com.example.data.LibrarySubView
import com.example.data.MediaItem
import com.example.data.ReleaseInfo
import com.example.data.ScrapeResult
import com.example.data.ScraprMeta
import com.example.data.ScraprUpdateInfo
import com.example.data.Track
import com.example.data.YtPlaylist
import com.example.download.DownloadManager
import com.example.engine.InnerTubeClient
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
    val authManager = YouTubeAuthManager(appContext)
    val repository = AppRepository(appContext)
    val player = YMusicPlayer(appContext) { authManager.buildAuthHeaders() }
    val downloadManager = DownloadManager(appContext, repository) { authManager.buildAuthHeaders() }

    private val _currentTab = MutableStateFlow(AppTab.HOME)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    // YouTube-style feed states
    private val _homeChip = MutableStateFlow(HomeChip.ALL)
    val homeChip: StateFlow<HomeChip> = _homeChip.asStateFlow()

    private val _homeFeed = MutableStateFlow<List<Track>>(emptyList())
    val homeFeed: StateFlow<List<Track>> = _homeFeed.asStateFlow()
    private val _homeLoading = MutableStateFlow(false)
    val homeLoading: StateFlow<Boolean> = _homeLoading.asStateFlow()
    private val _homeError = MutableStateFlow<String?>(null)
    val homeError: StateFlow<String?> = _homeError.asStateFlow()

    private val _trendingFeed = MutableStateFlow<List<Track>>(emptyList())
    val trendingFeed: StateFlow<List<Track>> = _trendingFeed.asStateFlow()
    private val _trendingLoading = MutableStateFlow(false)
    val trendingLoading: StateFlow<Boolean> = _trendingLoading.asStateFlow()
    private val _trendingError = MutableStateFlow<String?>(null)
    val trendingError: StateFlow<String?> = _trendingError.asStateFlow()

    private val _recommendedFeed = MutableStateFlow<List<Track>>(emptyList())
    val recommendedFeed: StateFlow<List<Track>> = _recommendedFeed.asStateFlow()
    private val _recommendedLoading = MutableStateFlow(false)
    val recommendedLoading: StateFlow<Boolean> = _recommendedLoading.asStateFlow()
    private val _recommendedError = MutableStateFlow<String?>(null)
    val recommendedError: StateFlow<String?> = _recommendedError.asStateFlow()

    private val _topSongsFeed = MutableStateFlow<List<Track>>(emptyList())
    val topSongsFeed: StateFlow<List<Track>> = _topSongsFeed.asStateFlow()
    private val _topSongsLoading = MutableStateFlow(false)
    val topSongsLoading: StateFlow<Boolean> = _topSongsLoading.asStateFlow()
    private val _topSongsError = MutableStateFlow<String?>(null)
    val topSongsError: StateFlow<String?> = _topSongsError.asStateFlow()

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

    // Google / YouTube Account & Sync state
    val accountProfile: StateFlow<AccountProfile> = authManager.profile

    private val _showLoginModal = MutableStateFlow(false)
    val showLoginModal: StateFlow<Boolean> = _showLoginModal.asStateFlow()

    private val _librarySubView = MutableStateFlow(LibrarySubView.OVERVIEW)
    val librarySubView: StateFlow<LibrarySubView> = _librarySubView.asStateFlow()

    private val _librarySection = MutableStateFlow(LibrarySection.LOCAL)
    val librarySection: StateFlow<LibrarySection> = _librarySection.asStateFlow()

    private val _likedTracks = MutableStateFlow<List<Track>>(emptyList())
    val likedTracks: StateFlow<List<Track>> = _likedTracks.asStateFlow()

    private val _userPlaylists = MutableStateFlow<List<YtPlaylist>>(emptyList())
    val userPlaylists: StateFlow<List<YtPlaylist>> = _userPlaylists.asStateFlow()

    private val _selectedPlaylist = MutableStateFlow<YtPlaylist?>(null)
    val selectedPlaylist: StateFlow<YtPlaylist?> = _selectedPlaylist.asStateFlow()

    private val _selectedPlaylistTracks = MutableStateFlow<List<Track>>(emptyList())
    val selectedPlaylistTracks: StateFlow<List<Track>> = _selectedPlaylistTracks.asStateFlow()

    private val _librarySyncing = MutableStateFlow(false)
    val librarySyncing: StateFlow<Boolean> = _librarySyncing.asStateFlow()

    private val _historySection = MutableStateFlow(HistorySection.SCRAPE)
    val historySection: StateFlow<HistorySection> = _historySection.asStateFlow()

    private val _ytHistoryTracks = MutableStateFlow<List<Track>>(emptyList())
    val ytHistoryTracks: StateFlow<List<Track>> = _ytHistoryTracks.asStateFlow()

    private val _historySyncing = MutableStateFlow(false)
    val historySyncing: StateFlow<Boolean> = _historySyncing.asStateFlow()

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
                    emitToast("Update v${info.tag} tersedia — buka Pengaturan")
                }
            }
        }
        loadHomeFeed()
        if (authManager.hasValidSession()) {
            viewModelScope.launch {
                refreshAccountProfile(silent = true)
                syncAccountLibrary(silent = true)
            }
        }
    }

    fun selectTab(tab: AppTab) {
        _currentTab.value = tab
        when (tab) {
            AppTab.HOME -> if (_homeFeed.value.isEmpty() && !_homeLoading.value) loadHomeFeed()
            AppTab.TRENDING -> if (_trendingFeed.value.isEmpty() && !_trendingLoading.value) loadTrendingFeed()
            AppTab.RECOMMENDED -> if (_recommendedFeed.value.isEmpty() && !_recommendedLoading.value) loadRecommendedFeed()
            AppTab.TOP_SONGS -> if (_topSongsFeed.value.isEmpty() && !_topSongsLoading.value) loadTopSongsFeed()
            AppTab.LIBRARY -> {
                if (authManager.hasValidSession() && _likedTracks.value.isEmpty() && _userPlaylists.value.isEmpty()) {
                    syncAccountLibrary(silent = true)
                }
            }
            AppTab.HISTORY -> {
                if (authManager.hasValidSession() &&
                    _historySection.value == HistorySection.YOUTUBE_MUSIC &&
                    _ytHistoryTracks.value.isEmpty()
                ) {
                    syncYtMusicHistory(silent = true)
                }
            }
            AppTab.SETTINGS -> {
                if (settings.value.autoUpdateEngine && _scraprInfo.value == null) {
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
            else -> Unit
        }
    }

    fun selectHomeChip(chip: HomeChip) {
        if (_homeChip.value == chip && _homeFeed.value.isNotEmpty()) return
        _homeChip.value = chip
        loadHomeFeed(force = true)
    }

    fun loadHomeFeed(force: Boolean = false) {
        if (_homeLoading.value && !force) return
        val chip = _homeChip.value
        viewModelScope.launch {
            _homeLoading.value = true
            _homeError.value = null
            try {
                val tracks = InnerTubeClient.fetchHomeFeed(
                    queryHint = chip.queryHint,
                    authHeaders = authManager.buildAuthHeaders(),
                )
                if (tracks.isEmpty()) {
                    _homeError.value = "Terjadi kesalahan (E20143)"
                } else {
                    _homeFeed.value = tracks
                }
            } catch (_: Exception) {
                _homeError.value = "Terjadi kesalahan (E20143)"
            } finally {
                _homeLoading.value = false
            }
        }
    }

    fun loadTrendingFeed() {
        if (_trendingLoading.value) return
        viewModelScope.launch {
            _trendingLoading.value = true
            _trendingError.value = null
            try {
                val tracks = InnerTubeClient.fetchTrendingMusic(authManager.buildAuthHeaders())
                if (tracks.isEmpty()) {
                    _trendingError.value = "Terjadi kesalahan (E20143)"
                } else {
                    _trendingFeed.value = tracks
                }
            } catch (_: Exception) {
                _trendingError.value = "Terjadi kesalahan (E20143)"
            } finally {
                _trendingLoading.value = false
            }
        }
    }

    fun loadRecommendedFeed() {
        if (_recommendedLoading.value) return
        viewModelScope.launch {
            _recommendedLoading.value = true
            _recommendedError.value = null
            try {
                val tracks = InnerTubeClient.fetchRecommendations(authManager.buildAuthHeaders())
                if (tracks.isEmpty()) {
                    _recommendedError.value = "Terjadi kesalahan (E20143)"
                } else {
                    _recommendedFeed.value = tracks
                }
            } catch (_: Exception) {
                _recommendedError.value = "Terjadi kesalahan (E20143)"
            } finally {
                _recommendedLoading.value = false
            }
        }
    }

    fun loadTopSongsFeed() {
        if (_topSongsLoading.value) return
        viewModelScope.launch {
            _topSongsLoading.value = true
            _topSongsError.value = null
            try {
                val tracks = InnerTubeClient.fetchTopSongs(authManager.buildAuthHeaders())
                if (tracks.isEmpty()) {
                    _topSongsError.value = "Terjadi kesalahan (E20143)"
                } else {
                    _topSongsFeed.value = tracks
                }
            } catch (_: Exception) {
                _topSongsError.value = "Terjadi kesalahan (E20143)"
            } finally {
                _topSongsLoading.value = false
            }
        }
    }

    fun openLoginModal() {
        _showLoginModal.value = true
    }

    fun closeLoginModal() {
        _showLoginModal.value = false
    }

    fun onLoginCookiesCaptured(rawCookies: String) {
        val ok = authManager.saveSessionFromWebView(rawCookies)
        _showLoginModal.value = false
        if (!ok) {
            emitToast("Cookie sesi belum lengkap, coba login ulang")
            return
        }
        emitToast("Login berhasil, menyinkronkan akun…")
        viewModelScope.launch {
            refreshAccountProfile(silent = false)
            syncAccountLibrary(silent = true)
            syncYtMusicHistory(silent = true)
            loadHomeFeed(force = true)
        }
    }

    fun refreshAccountProfile(silent: Boolean = false) {
        if (!authManager.hasValidSession()) {
            if (!silent) emitToast("Silakan login akun Google terlebih dahulu")
            return
        }
        viewModelScope.launch {
            val fetched = InnerTubeClient.fetchAccountProfile(authManager.buildAuthHeaders())
            if (fetched != null) {
                authManager.saveProfile(fetched)
                if (!silent) emitToast("Profil disinkronkan: ${fetched.name}")
            } else if (!silent) {
                emitToast("Gagal mengambil profil akun")
            }
        }
    }

    fun logoutAccount() {
        authManager.logout()
        _likedTracks.value = emptyList()
        _userPlaylists.value = emptyList()
        _selectedPlaylist.value = null
        _selectedPlaylistTracks.value = emptyList()
        _ytHistoryTracks.value = emptyList()
        emitToast("Berhasil logout dari akun Google")
    }

    fun openLibrarySubView(subView: LibrarySubView) {
        _librarySubView.value = subView
        when (subView) {
            LibrarySubView.LOCAL -> _librarySection.value = LibrarySection.LOCAL
            LibrarySubView.LIKED_MUSIC -> {
                _librarySection.value = LibrarySection.LIKED_MUSIC
                if (authManager.hasValidSession() && _likedTracks.value.isEmpty()) {
                    syncAccountLibrary(silent = true)
                }
            }
            LibrarySubView.PLAYLISTS -> {
                _librarySection.value = LibrarySection.PLAYLISTS
                if (authManager.hasValidSession() && _userPlaylists.value.isEmpty()) {
                    syncAccountLibrary(silent = true)
                }
            }
            LibrarySubView.OVERVIEW -> {
                _selectedPlaylist.value = null
                _selectedPlaylistTracks.value = emptyList()
            }
        }
    }

    fun closeLibrarySubView() {
        if (_selectedPlaylist.value != null) {
            closePlaylist()
        } else {
            _librarySubView.value = LibrarySubView.OVERVIEW
        }
    }

    fun selectLibrarySection(section: LibrarySection) {
        _librarySection.value = section
        if (section != LibrarySection.PLAYLISTS) {
            _selectedPlaylist.value = null
            _selectedPlaylistTracks.value = emptyList()
        }
        if (section != LibrarySection.LOCAL && authManager.hasValidSession() &&
            _likedTracks.value.isEmpty() && _userPlaylists.value.isEmpty()
        ) {
            syncAccountLibrary(silent = true)
        }
    }

    fun syncAccountLibrary(silent: Boolean = false) {
        if (!authManager.hasValidSession()) {
            if (!silent) emitToast("Login akun Google dulu di Perpustakaan / Pengaturan")
            return
        }
        if (_librarySyncing.value) return
        viewModelScope.launch {
            _librarySyncing.value = true
            try {
                val headers = authManager.buildAuthHeaders()
                val liked = MusicSearchEngine.fetchLikedMusic(headers)
                    .ifEmpty { MusicSearchEngine.fetchLibrarySongs(headers) }
                val playlists = MusicSearchEngine.fetchUserPlaylists(headers)
                _likedTracks.value = liked
                _userPlaylists.value = playlists
                if (!silent) {
                    emitToast("Sinkronisasi: ${liked.size} lagu, ${playlists.size} playlist")
                }
            } catch (e: Exception) {
                if (!silent) emitToast("Gagal sinkronisasi: ${e.message ?: "error"}")
            } finally {
                _librarySyncing.value = false
            }
        }
    }

    fun openPlaylist(playlist: YtPlaylist) {
        _librarySubView.value = LibrarySubView.PLAYLISTS
        _librarySection.value = LibrarySection.PLAYLISTS
        _selectedPlaylist.value = playlist
        _selectedPlaylistTracks.value = emptyList()
        viewModelScope.launch {
            _librarySyncing.value = true
            try {
                val tracks = MusicSearchEngine.fetchPlaylistTracks(
                    playlist.browseId,
                    authManager.buildAuthHeaders(),
                )
                _selectedPlaylistTracks.value = tracks
            } catch (e: Exception) {
                emitToast("Gagal memuat playlist: ${e.message ?: "error"}")
            } finally {
                _librarySyncing.value = false
            }
        }
    }

    fun closePlaylist() {
        _selectedPlaylist.value = null
        _selectedPlaylistTracks.value = emptyList()
    }

    fun selectHistorySection(section: HistorySection) {
        _historySection.value = section
        if (section == HistorySection.YOUTUBE_MUSIC && authManager.hasValidSession() && _ytHistoryTracks.value.isEmpty()) {
            syncYtMusicHistory(silent = true)
        }
    }

    fun syncYtMusicHistory(silent: Boolean = false) {
        if (!authManager.hasValidSession()) {
            if (!silent) emitToast("Login akun Google dulu untuk riwayat YT Music")
            return
        }
        if (_historySyncing.value) return
        viewModelScope.launch {
            _historySyncing.value = true
            try {
                val items = MusicSearchEngine.fetchUserHistory(authManager.buildAuthHeaders())
                _ytHistoryTracks.value = items
                if (!silent) emitToast("Riwayat YT Music: ${items.size} lagu")
            } catch (e: Exception) {
                if (!silent) emitToast("Gagal memuat riwayat: ${e.message ?: "error"}")
            } finally {
                _historySyncing.value = false
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
                val result = MediaScraper.scrapeUrl(url, authManager.buildAuthHeaders())
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
            val format = if (item.type == "audio" || item.ext in setOf("mp3", "m4a", "opus", "wav")) {
                DownloadFormat.MP3
            } else {
                DownloadFormat.MP4
            }
            downloadManager.enqueue(withItemUrl, format)
            if (MediaScraper.isDirectMediaUrl(item.url) || !track.videoId.isNullOrBlank()) {
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
            val res = MusicSearchEngine.searchTracks(
                query = trimmed,
                apiKey = settings.value.youtubeApiKey,
                authHeaders = authManager.buildAuthHeaders(),
            )
            _searchResults.value = res.tracks
            _isDemoSearch.value = res.isDemo
            _isSearching.value = false
        }
    }

    fun playTrack(track: Track) {
        if (track.videoId.isNullOrBlank() && !track.playlistBrowseId.isNullOrBlank()) {
            emitToast("Memuat Mix: ${track.title}")
            viewModelScope.launch {
                val items = runCatching {
                    MusicSearchEngine.fetchPlaylistTracks(
                        track.playlistBrowseId,
                        authManager.buildAuthHeaders(),
                    )
                }.getOrDefault(emptyList())
                val firstPlayable = items.firstOrNull { !it.videoId.isNullOrBlank() }
                if (firstPlayable != null) {
                    player.play(firstPlayable)
                    emitToast("Memutar: ${firstPlayable.title}")
                } else {
                    emitToast("Tidak ada lagu yang bisa diputar di Mix ini")
                }
            }
            return
        }
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
