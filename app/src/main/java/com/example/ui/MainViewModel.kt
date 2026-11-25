package com.example.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.YMusicApplication
import com.example.auth.YouTubeAuthManager
import com.example.data.AccountProfile
import com.example.data.AppRepository
import com.example.data.AppSettings
import com.example.data.AppTab
import com.example.data.CommentsPageResult
import com.example.data.DownloadFormat
import com.example.data.DownloadJob
import com.example.data.DownloadStatus
import com.example.data.EngineMeta
import com.example.data.HistorySection
import com.example.data.HomeChip
import com.example.data.LibrarySection
import com.example.data.LibrarySubView
import com.example.data.LyricsState
import com.example.data.MediaItem
import com.example.data.PreviewKind
import com.example.data.PreviewTarget
import com.example.data.QualityPickerRequest
import com.example.data.ReleaseInfo
import com.example.data.ResolvedStreams
import com.example.data.ScrapeResult
import com.example.data.ScraperServer
import com.example.data.ScraperServerKind
import com.example.data.ScraprMeta
import com.example.data.ScraprUpdateInfo
import com.example.data.ServerStatus
import com.example.data.StreamCandidate
import com.example.data.Track
import com.example.data.TrackSource
import com.example.data.YtPlaylist
import com.example.debug.AppDiagnostics
import com.example.download.DownloadManager
import com.example.engine.InnerTubeClient
import com.example.engine.InnerTubeSocialClient
import com.example.engine.MediaScraper
import com.example.engine.MusicSearchEngine
import com.example.engine.PlatformDetector
import com.example.engine.ServerManager
import com.example.notify.AppNotifications
import com.example.player.PlayerState
import com.example.player.YMusicPlayer
import com.example.share.ShareHelper
import dev.bagja.spatialaudio.audio.SpatialHostBridge
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
    private val app = application as YMusicApplication
    private val appContext = application.applicationContext
    val authManager: YouTubeAuthManager = app.authManager
    val repository: AppRepository = app.repository
    val player: YMusicPlayer = app.player
    val downloadManager: DownloadManager = app.downloadManager

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

    private val _previewTarget = MutableStateFlow<PreviewTarget?>(null)
    val previewTarget: StateFlow<PreviewTarget?> = _previewTarget.asStateFlow()

    // Server Menu state
    private val _serverStatuses = MutableStateFlow<Map<String, ScraperServer>>(emptyMap())
    val serverStatuses: StateFlow<Map<String, ScraperServer>> = _serverStatuses.asStateFlow()

    private val _pingingServers = MutableStateFlow(false)
    val pingingServers: StateFlow<Boolean> = _pingingServers.asStateFlow()

    // Quality & MB Picker state
    data class QualityPickerUiState(
        val request: QualityPickerRequest,
        val title: String,
        val subtitle: String,
        val thumbnail: String,
        val resolvedStreams: ResolvedStreams? = null,
        val scrapeResult: ScrapeResult? = null,
        val isLoading: Boolean = true,
        val errorMessage: String? = null,
    )

    private val _qualityPickerState = MutableStateFlow<QualityPickerUiState?>(null)
    val qualityPickerState: StateFlow<QualityPickerUiState?> = _qualityPickerState.asStateFlow()

    // Full-screen Now Playing sheet & YouTube Social / Lyrics state
    private val _nowPlayingOpen = MutableStateFlow(false)
    val nowPlayingOpen: StateFlow<Boolean> = _nowPlayingOpen.asStateFlow()

    private val _commentsVideoId = MutableStateFlow<String?>(null)
    private val _commentsState = MutableStateFlow(CommentsPageResult())
    val commentsState: StateFlow<CommentsPageResult> = _commentsState.asStateFlow()

    private val _commentsLoading = MutableStateFlow(false)
    val commentsLoading: StateFlow<Boolean> = _commentsLoading.asStateFlow()

    private val _commentPosting = MutableStateFlow(false)
    val commentPosting: StateFlow<Boolean> = _commentPosting.asStateFlow()

    private val _lyricsState = MutableStateFlow(LyricsState())
    val lyricsState: StateFlow<LyricsState> = _lyricsState.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    private var searchJob: Job? = null
    private var qualityPickerJob: Job? = null

    init {
        SpatialHostBridge.onSharedText = { text ->
            handleSharedText(text)
        }
        viewModelScope.launch {
            app.globalToast.collect { msg ->
                _toastMessage.tryEmit(msg)
            }
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
                val activeSrv = getActiveServer()
                val result = MediaScraper.scrapeUrl(url, authManager.buildAuthHeaders(), activeSrv)
                _lastScrape.value = result
                if (result.status) {
                    val updated = (listOf(result) + history.value).take(50)
                    repository.saveHistory(updated)
                }
                emitToast(if (result.status) "Berhasil (${activeSrv.name})" else (result.error ?: "Gagal"))
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
            downloadManager.enqueue(withItemUrl, format, overrideExt = item.ext)
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

    fun playTrack(track: Track, queueContext: List<Track>? = null) {
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
                    player.play(firstPlayable, items)
                    emitToast("Memutar: ${firstPlayable.title}")
                } else {
                    emitToast("Tidak ada lagu yang bisa diputar di Mix ini")
                }
            }
            return
        }
        player.play(track, queueContext)
        emitToast("Memutar: ${track.title}")
    }

    fun addToQueueNext(track: Track) {
        player.addToQueueNext(track)
        emitToast("Ditambah ke antrean berikutnya: ${track.title}")
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
        openQualityPicker(track, format)
    }

    // ---- Server Menu management ----

    fun getAllServers(): List<ScraperServer> {
        val statusMap = _serverStatuses.value
        return ServerManager.allServers(settings.value).map { statusMap[it.id] ?: it }
    }

    fun getActiveServer(): ScraperServer =
        ServerManager.resolveActiveServer(settings.value)

    fun selectServer(serverId: String) {
        viewModelScope.launch {
            repository.updateSettings { it.copy(activeServerId = serverId) }
            val picked = ServerManager.allServers(settings.value).firstOrNull { it.id == serverId }
            emitToast("Server aktif: ${picked?.name ?: serverId}")
            if (_qualityPickerState.value != null) {
                loadQualityPickerStreams(serverId)
            }
            if (_currentTab.value == AppTab.DOWNLOAD && _urlInput.value.isNotBlank() && _lastScrape.value != null) {
                scrapeCurrentUrl()
            }
        }
    }

    fun pingAllServers() {
        if (_pingingServers.value) return
        viewModelScope.launch {
            _pingingServers.value = true
            try {
                val list = ServerManager.allServers(settings.value)
                val updated = coroutineScope {
                    list.map { srv -> async { ServerManager.pingServer(srv) } }.awaitAll()
                }
                _serverStatuses.value = updated.associateBy { it.id }
                val onlineCount = updated.count { it.lastStatus == ServerStatus.ONLINE }
                emitToast("Ping selesai: $onlineCount/${updated.size} server online")
            } finally {
                _pingingServers.value = false
            }
        }
    }

    fun addCustomServer(name: String, baseUrl: String) {
        val cleanUrl = baseUrl.trim()
        if (!cleanUrl.startsWith("https://")) {
            emitToast("URL server harus diawali https://")
            return
        }
        val cleanName = name.trim().ifBlank { cleanUrl.removePrefix("https://").substringBefore('/') }
        val newServer = ScraperServer(
            id = "custom-${System.currentTimeMillis()}",
            name = cleanName,
            kind = ScraperServerKind.CUSTOM_API,
            baseUrl = cleanUrl,
            description = "Server API HTTPS kustom ($cleanUrl)",
            supportsMultiPlatform = true,
            isBuiltIn = false,
        )
        viewModelScope.launch {
            repository.updateSettings { s ->
                s.copy(
                    customServers = s.customServers + newServer,
                    activeServerId = newServer.id,
                )
            }
            emitToast("Server '$cleanName' disimpan & dipilih")
        }
    }

    fun removeCustomServer(serverId: String) {
        viewModelScope.launch {
            repository.updateSettings { s ->
                val remaining = s.customServers.filterNot { it.id == serverId }
                val nextActive = if (s.activeServerId == serverId) "auto" else s.activeServerId
                s.copy(customServers = remaining, activeServerId = nextActive)
            }
            emitToast("Server kustom dihapus")
        }
    }

    // ---- Universal Quality & MB Picker ----

    fun openQualityPicker(track: Track, initialFormat: DownloadFormat? = null) {
        val req = QualityPickerRequest(
            track = track,
            rawUrl = track.videoId?.let { "https://www.youtube.com/watch?v=$it" } ?: track.url.orEmpty(),
            initialFormat = initialFormat,
        )
        _qualityPickerState.value = QualityPickerUiState(
            request = req,
            title = track.title,
            subtitle = track.artist,
            thumbnail = track.thumbnail,
            isLoading = true,
        )
        loadQualityPickerStreams(settings.value.activeServerId)
    }

    fun openQualityPickerForScrape(scrape: ScrapeResult, initialFormat: DownloadFormat? = null) {
        val track = MediaScraper.scrapeToTrack(scrape)
        val req = QualityPickerRequest(
            track = track,
            rawUrl = scrape.rawUrl,
            initialFormat = initialFormat,
        )
        _qualityPickerState.value = QualityPickerUiState(
            request = req,
            title = scrape.title.ifBlank { scrape.platformLabel },
            subtitle = scrape.author ?: scrape.platformLabel,
            thumbnail = scrape.thumbnail.orEmpty(),
            scrapeResult = scrape,
            isLoading = !track?.videoId.isNullOrBlank(),
        )
        if (!track?.videoId.isNullOrBlank()) {
            loadQualityPickerStreams(settings.value.activeServerId)
        }
    }

    fun switchPickerServer(serverId: String) {
        viewModelScope.launch {
            repository.updateSettings { it.copy(activeServerId = serverId) }
            loadQualityPickerStreams(serverId)
        }
    }

    fun retryQualityPicker() {
        loadQualityPickerStreams(settings.value.activeServerId)
    }

    private fun loadQualityPickerStreams(serverId: String) {
        val current = _qualityPickerState.value ?: return
        qualityPickerJob?.cancel()
        qualityPickerJob = viewModelScope.launch {
            _qualityPickerState.value = current.copy(isLoading = true, errorMessage = null)
            val srv = ServerManager.resolveActiveServer(settings.value, serverId)
            val headers = authManager.buildAuthHeaders()
            val vid = current.request.track?.videoId?.takeIf { it.isNotBlank() }
                ?: current.request.rawUrl.takeIf { it.isNotBlank() }?.let { PlatformDetector.extractYoutubeId(it) }

            if (!vid.isNullOrBlank()) {
                val resolved = runCatching { ServerManager.resolveWithServer(vid, srv, headers) }.getOrNull()
                if (resolved != null && (resolved.audioStreams.isNotEmpty() || resolved.videoStreams.isNotEmpty())) {
                    _qualityPickerState.value = _qualityPickerState.value?.copy(
                        resolvedStreams = resolved,
                        isLoading = false,
                        errorMessage = null,
                    )
                    return@launch
                }
            }

            val rawUrl = current.request.rawUrl
            if (rawUrl.isNotBlank()) {
                val scrape = runCatching { MediaScraper.scrapeUrl(rawUrl, headers, srv) }.getOrNull()
                if (scrape != null && scrape.items.isNotEmpty()) {
                    _qualityPickerState.value = _qualityPickerState.value?.copy(
                        scrapeResult = scrape,
                        isLoading = false,
                        errorMessage = scrape.error,
                    )
                    return@launch
                }
            }

            _qualityPickerState.value = _qualityPickerState.value?.copy(
                isLoading = false,
                errorMessage = "Gagal mengambil daftar resolusi dari ${srv.name}. Coba ganti server lain di atas.",
            )
        }
    }

    fun closeQualityPicker() {
        qualityPickerJob?.cancel()
        _qualityPickerState.value = null
    }

    fun downloadStreamCandidate(candidate: StreamCandidate) {
        val state = _qualityPickerState.value ?: return
        val baseTrack = state.request.track ?: Track(
            id = state.resolvedStreams?.videoId ?: "dl-${System.currentTimeMillis()}",
            title = state.title,
            artist = state.subtitle,
            thumbnail = state.thumbnail,
            source = if (!state.resolvedStreams?.videoId.isNullOrBlank()) TrackSource.YOUTUBE else TrackSource.URL,
            videoId = state.resolvedStreams?.videoId,
            url = candidate.url,
        )
        val format = if (candidate.isAudio) DownloadFormat.MP3 else DownloadFormat.MP4
        downloadManager.enqueue(
            track = baseTrack.copy(url = candidate.url),
            format = format,
            overrideExt = candidate.ext,
            overrideUserAgent = candidate.userAgent.ifBlank { null },
        )
        emitToast("Mengunduh ${candidate.qualityLabel} (${candidate.formattedSize.ifBlank { candidate.ext.uppercase() }})")
        closeQualityPicker()
    }

    fun downloadPickerMediaItem(item: MediaItem) {
        val state = _qualityPickerState.value ?: return
        val baseTrack = state.request.track ?: state.scrapeResult?.let { MediaScraper.scrapeToTrack(it) } ?: Track(
            id = "dl-${System.currentTimeMillis()}",
            title = state.title,
            artist = state.subtitle,
            thumbnail = state.thumbnail,
            source = TrackSource.URL,
            url = item.url,
        )
        val isAudio = item.type == "audio" || item.ext?.lowercase() in setOf("mp3", "m4a", "opus", "ogg", "wav")
        val format = if (isAudio) DownloadFormat.MP3 else DownloadFormat.MP4
        downloadManager.enqueue(
            track = baseTrack.copy(url = item.url),
            format = format,
            overrideExt = item.ext,
        )
        emitToast("Download ${item.quality ?: format.label} dimulai")
        closeQualityPicker()
    }

    fun previewStreamCandidate(candidate: StreamCandidate) {
        val state = _qualityPickerState.value ?: return
        openPreview(
            PreviewTarget(
                id = "qp-prev-${candidate.url.hashCode()}",
                title = state.title,
                subtitle = "${candidate.qualityLabel} · ${candidate.formattedSize}",
                urlOrUri = candidate.url,
                mimeType = candidate.mimeType,
                ext = candidate.ext,
                kind = if (candidate.isAudio) PreviewKind.AUDIO else PreviewKind.VIDEO,
                thumbnail = state.thumbnail,
                videoId = state.resolvedStreams?.videoId ?: state.request.track?.videoId,
                userAgent = candidate.userAgent.ifBlank { "YMusic-Lite/0.3.0" },
            ),
        )
    }

    fun previewPickerMediaItem(item: MediaItem) {
        val state = _qualityPickerState.value ?: return
        val scrape = state.scrapeResult
        if (scrape != null) {
            previewMediaItem(scrape, item)
        } else {
            val ext = item.ext.orEmpty()
            openPreview(
                PreviewTarget(
                    id = "qp-item-${item.url.hashCode()}",
                    title = state.title,
                    subtitle = item.quality.orEmpty(),
                    urlOrUri = item.url,
                    ext = ext,
                    kind = PreviewTarget.detectKind(null, ext, item.type),
                    thumbnail = state.thumbnail,
                    videoId = state.request.track?.videoId,
                ),
            )
        }
    }

    // ---- Now Playing Sheet, Lyrics, YouTube Comments & Like, Share ----

    fun openNowPlaying() {
        _nowPlayingOpen.value = true
        val vid = playerState.value.track?.videoId
        if (!vid.isNullOrBlank()) {
            if (_commentsVideoId.value != vid) loadCommentsForTrack(vid)
            if (_lyricsState.value.videoId != vid) loadLyricsForTrack(vid)
        }
    }

    fun closeNowPlaying() {
        _nowPlayingOpen.value = false
    }

    fun loadCommentsForTrack(videoId: String) {
        if (videoId.isBlank()) return
        _commentsVideoId.value = videoId
        viewModelScope.launch {
            _commentsLoading.value = true
            val res = InnerTubeSocialClient.fetchCommentsAndLikeState(
                videoId = videoId,
                authHeaders = authManager.buildAuthHeaders(),
            )
            _commentsState.value = res
            _commentsLoading.value = false
        }
    }

    fun loadMoreComments() {
        val vid = _commentsVideoId.value ?: return
        val token = _commentsState.value.continuationToken ?: return
        if (_commentsLoading.value) return
        viewModelScope.launch {
            _commentsLoading.value = true
            val next = InnerTubeSocialClient.fetchCommentsAndLikeState(
                videoId = vid,
                authHeaders = authManager.buildAuthHeaders(),
                continuationToken = token,
            )
            val prev = _commentsState.value
            _commentsState.value = prev.copy(
                comments = prev.comments + next.comments,
                continuationToken = next.continuationToken,
                createCommentParams = prev.createCommentParams ?: next.createCommentParams,
            )
            _commentsLoading.value = false
        }
    }

    fun toggleLikeCurrentTrack() {
        val vid = playerState.value.track?.videoId ?: return
        if (!authManager.hasValidSession()) {
            openLoginModal()
            return
        }
        val currentlyLiked = _commentsState.value.isLikedByMe == true
        val targetLike = !currentlyLiked
        viewModelScope.launch {
            val ok = InnerTubeSocialClient.toggleLikeVideo(vid, targetLike, authManager.buildAuthHeaders())
            if (ok) {
                _commentsState.value = _commentsState.value.copy(isLikedByMe = targetLike)
                emitToast(if (targetLike) "Disukai di YouTube" else "Batal menyukai")
            } else {
                emitToast("Gagal mengubah status Like YouTube")
            }
        }
    }

    fun submitComment(commentText: String) {
        val vid = playerState.value.track?.videoId ?: return
        if (!authManager.hasValidSession()) {
            openLoginModal()
            return
        }
        if (_commentPosting.value) return
        viewModelScope.launch {
            _commentPosting.value = true
            val res = InnerTubeSocialClient.createComment(
                videoId = vid,
                commentText = commentText,
                createCommentParams = _commentsState.value.createCommentParams,
                authHeaders = authManager.buildAuthHeaders(),
            )
            _commentPosting.value = false
            res.onSuccess { posted ->
                _commentsState.value = _commentsState.value.copy(
                    comments = listOf(posted) + _commentsState.value.comments,
                )
                emitToast("Komentar terkirim ke YouTube")
            }.onFailure { err ->
                emitToast(err.message ?: "Gagal mengirim komentar")
            }
        }
    }

    fun loadLyricsForTrack(videoId: String) {
        if (videoId.isBlank()) return
        viewModelScope.launch {
            _lyricsState.value = LyricsState(videoId = videoId, isLoading = true)
            val res = InnerTubeSocialClient.fetchLyrics(videoId, authManager.buildAuthHeaders())
            res.onSuccess { text ->
                _lyricsState.value = LyricsState(videoId = videoId, text = text, sourceLabel = "YouTube Music", isLoading = false)
            }.onFailure { err ->
                _lyricsState.value = LyricsState(videoId = videoId, error = err.message ?: "Lirik tidak tersedia", isLoading = false)
            }
        }
    }

    fun shareTrack(track: Track) {
        ShareHelper.shareTrackLink(appContext, track)
    }

    fun shareDownloadJob(job: DownloadJob) {
        ShareHelper.shareDownloadedJob(appContext, job)
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

    fun handleNotificationIntent(intent: Intent?) {
        val route = intent?.getStringExtra(AppNotifications.EXTRA_NAV_ROUTE) ?: return
        AppDiagnostics.info("Navigation", "Notification route -> $route")
        when (route) {
            AppNotifications.ROUTE_PLAYER -> {
                openNowPlaying()
            }
            AppNotifications.ROUTE_SPATIAL -> {
                selectTab(AppTab.SPATIAL)
            }
            AppNotifications.ROUTE_DOWNLOAD_QUEUE -> {
                selectTab(AppTab.DOWNLOAD)
            }
            AppNotifications.ROUTE_DOWNLOAD_PREVIEW -> {
                selectTab(AppTab.DOWNLOAD)
                val jobId = intent.getStringExtra(AppNotifications.EXTRA_JOB_ID)
                val job = jobId?.let { id -> downloadJobs.value.firstOrNull { it.id == id } }
                if (job != null) {
                    previewDownloadJob(job)
                } else {
                    val uri = intent.getStringExtra(AppNotifications.EXTRA_PREVIEW_URI)
                    if (!uri.isNullOrBlank()) {
                        val title = intent.getStringExtra(AppNotifications.EXTRA_PREVIEW_TITLE) ?: "Pratinjau File"
                        val mime = intent.getStringExtra(AppNotifications.EXTRA_PREVIEW_MIME).orEmpty()
                        val ext = uri.substringAfterLast('.', "")
                        openPreview(
                            PreviewTarget(
                                id = jobId ?: "notif-${System.currentTimeMillis()}",
                                title = title,
                                subtitle = mime.ifBlank { "Hasil Unduhan" },
                                urlOrUri = uri,
                                mimeType = mime,
                                ext = ext,
                                kind = PreviewTarget.detectKind(mime, ext),
                                downloadJobId = jobId,
                            ),
                        )
                    }
                }
            }
            AppNotifications.ROUTE_DEBUG -> {
                selectTab(AppTab.DEBUG)
            }
        }
    }

    fun openPreview(target: PreviewTarget) {
        if (player.state.value.playing) {
            player.toggle()
        }
        _previewTarget.value = target
    }

    fun closePreview() {
        _previewTarget.value = null
    }

    fun previewMediaItem(scrape: ScrapeResult, item: MediaItem) {
        val ext = item.ext.orEmpty()
        val targetKind = PreviewTarget.detectKind(null, ext, item.type)
        val vid = if (scrape.platform == com.example.data.PlatformId.YOUTUBE) {
            com.example.engine.PlatformDetector.extractYoutubeId(scrape.rawUrl)
        } else {
            null
        }
        openPreview(
            PreviewTarget(
                id = "item-${item.url.hashCode()}",
                title = scrape.title.ifBlank { "Pratinjau Media" },
                subtitle = item.quality ?: scrape.platformLabel,
                urlOrUri = item.url,
                ext = ext,
                kind = targetKind,
                thumbnail = scrape.thumbnail.orEmpty(),
                videoId = vid,
            ),
        )
    }

    fun previewDownloadJob(job: DownloadJob) {
        val localUri = job.contentUri?.takeIf { it.isNotBlank() } ?: job.filePath?.takeIf { it.startsWith("/") }
        if (!localUri.isNullOrBlank()) {
            val ext = job.filePath?.substringAfterLast('.', "")?.ifBlank { job.format.ext } ?: job.format.ext
            val mime = job.mimeType.orEmpty()
            openPreview(
                PreviewTarget(
                    id = job.id,
                    title = job.track.title,
                    subtitle = job.filePath ?: job.format.label,
                    urlOrUri = localUri,
                    mimeType = mime,
                    ext = ext,
                    kind = PreviewTarget.detectKind(mime, ext),
                    thumbnail = job.track.thumbnail,
                    videoId = job.track.videoId,
                    downloadJobId = job.id,
                ),
            )
            return
        }

        val remoteUrl = job.track.url
        if (!remoteUrl.isNullOrBlank() && MediaScraper.isDirectMediaUrl(remoteUrl)) {
            val ext = job.format.ext
            openPreview(
                PreviewTarget(
                    id = job.id,
                    title = job.track.title,
                    subtitle = job.track.artist,
                    urlOrUri = remoteUrl,
                    ext = ext,
                    kind = if (job.format == DownloadFormat.MP3) PreviewKind.AUDIO else PreviewKind.VIDEO,
                    thumbnail = job.track.thumbnail,
                    videoId = job.track.videoId,
                    downloadJobId = job.id,
                ),
            )
            return
        }

        if (!job.track.videoId.isNullOrBlank()) {
            viewModelScope.launch {
                val resolved = runCatching {
                    InnerTubeClient.resolveStreams(job.track.videoId, authManager.buildAuthHeaders())
                }.getOrNull()
                val stream = if (job.format == DownloadFormat.MP3) {
                    resolved?.bestAudio
                } else {
                    resolved?.bestVideo ?: resolved?.bestAudio
                }
                if (stream != null) {
                    openPreview(
                        PreviewTarget(
                            id = job.id,
                            title = job.track.title,
                            subtitle = stream.qualityLabel,
                            urlOrUri = stream.url,
                            mimeType = stream.mimeType,
                            ext = stream.ext,
                            kind = if (stream.isAudio) PreviewKind.AUDIO else PreviewKind.VIDEO,
                            thumbnail = job.track.thumbnail,
                            videoId = job.track.videoId,
                            downloadJobId = job.id,
                            userAgent = stream.userAgent.ifBlank { "YMusic-Lite/0.3.0" },
                        ),
                    )
                } else {
                    emitToast("Stream pratinjau tidak tersedia")
                }
            }
        } else {
            emitToast("Berkas lokal belum selesai diunduh")
        }
    }

    fun playPreviewTargetIn8D(target: PreviewTarget) {
        val job = target.downloadJobId?.let { id -> downloadJobs.value.firstOrNull { it.id == id } }
        val track = job?.track?.copy(url = target.urlOrUri)
            ?: Track(
                id = target.videoId ?: target.id,
                title = target.title,
                artist = target.subtitle.ifBlank { "Preview 8D" },
                thumbnail = target.thumbnail,
                source = if (!target.videoId.isNullOrBlank()) TrackSource.YOUTUBE else TrackSource.LOCAL,
                videoId = target.videoId,
                url = target.urlOrUri,
            )
        playTrack(track)
        selectTab(AppTab.SPATIAL)
    }

    fun emitToast(msg: String) {
        _toastMessage.tryEmit(msg)
    }

    override fun onCleared() {
        super.onCleared()
        SpatialHostBridge.onSharedText = null
    }
}
