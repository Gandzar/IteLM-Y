package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SpatialAudioOff
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SpatialAudioOff
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.data.AppTab
import com.example.data.LibrarySubView
import com.example.player.PlayerMode
import com.example.ui.components.GoogleLoginDialog
import com.example.ui.components.MiniPlayerBar
import com.example.ui.components.TrackOptionsBottomSheet
import com.example.ui.components.YouTubeEmbedOverlay
import com.example.ui.screens.DownloadScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SpatialScreen
import com.example.ui.screens.YouTubeFeedScreen
import com.example.ui.theme.SpatialBg
import com.example.ui.theme.SpatialGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.YtBg
import com.example.ui.theme.YtCard
import com.example.ui.theme.YtDivider
import com.example.ui.theme.YtElevated
import com.example.ui.theme.YtOrange
import com.example.ui.theme.YtOrangeSoft
import com.example.ui.theme.YtTextMuted
import com.example.ui.theme.YtTextPrimary
import com.example.ui.theme.YtTextSecondary
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

private data class TabMeta(
    val tab: AppTab,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

private val PRIMARY_TABS = listOf(
    TabMeta(AppTab.HOME, "Beranda", Icons.Filled.Home, Icons.Outlined.Home),
    TabMeta(AppTab.TRENDING, "Musik trending", Icons.Filled.MusicNote, Icons.Outlined.MusicNote),
    TabMeta(AppTab.RECOMMENDED, "Rekomendasi", Icons.Filled.Star, Icons.Outlined.StarOutline),
    TabMeta(AppTab.TOP_SONGS, "Lagu teratas", Icons.AutoMirrored.Filled.TrendingUp, Icons.AutoMirrored.Filled.TrendingUp),
    TabMeta(AppTab.LIBRARY, "Perpustakaan", Icons.Filled.Person, Icons.Outlined.Person),
)

private val DRAWER_TOOL_TABS = listOf(
    TabMeta(AppTab.SEARCH, "Cari Lagu & Video", Icons.Filled.Search, Icons.Outlined.Search),
    TabMeta(AppTab.DOWNLOAD, "All Downloader", Icons.Filled.Download, Icons.Outlined.Download),
    TabMeta(AppTab.SPATIAL, "Spatial 8D Studio", Icons.Filled.SpatialAudioOff, Icons.Outlined.SpatialAudioOff),
    TabMeta(AppTab.HISTORY, "Riwayat & Scrape", Icons.Filled.History, Icons.Outlined.History),
    TabMeta(AppTab.SETTINGS, "Pengaturan", Icons.Filled.Settings, Icons.Outlined.Settings),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YMusicApp(
    mainVm: MainViewModel = viewModel(),
    spatialVm: SpatialViewModel = viewModel(),
) {
    val currentTab by mainVm.currentTab.collectAsStateWithLifecycle()

    // Feed states
    val homeChip by mainVm.homeChip.collectAsStateWithLifecycle()
    val homeFeed by mainVm.homeFeed.collectAsStateWithLifecycle()
    val homeLoading by mainVm.homeLoading.collectAsStateWithLifecycle()
    val homeError by mainVm.homeError.collectAsStateWithLifecycle()
    val trendingFeed by mainVm.trendingFeed.collectAsStateWithLifecycle()
    val trendingLoading by mainVm.trendingLoading.collectAsStateWithLifecycle()
    val trendingError by mainVm.trendingError.collectAsStateWithLifecycle()
    val recommendedFeed by mainVm.recommendedFeed.collectAsStateWithLifecycle()
    val recommendedLoading by mainVm.recommendedLoading.collectAsStateWithLifecycle()
    val recommendedError by mainVm.recommendedError.collectAsStateWithLifecycle()
    val topSongsFeed by mainVm.topSongsFeed.collectAsStateWithLifecycle()
    val topSongsLoading by mainVm.topSongsLoading.collectAsStateWithLifecycle()
    val topSongsError by mainVm.topSongsError.collectAsStateWithLifecycle()

    val urlInput by mainVm.urlInput.collectAsStateWithLifecycle()
    val scrapeBusy by mainVm.scrapeBusy.collectAsStateWithLifecycle()
    val lastScrape by mainVm.lastScrape.collectAsStateWithLifecycle()
    val searchQuery by mainVm.searchQuery.collectAsStateWithLifecycle()
    val searchResults by mainVm.searchResults.collectAsStateWithLifecycle()
    val isSearching by mainVm.isSearching.collectAsStateWithLifecycle()
    val isDemoSearch by mainVm.isDemoSearch.collectAsStateWithLifecycle()
    val library by mainVm.library.collectAsStateWithLifecycle()
    val history by mainVm.history.collectAsStateWithLifecycle()
    val downloadJobs by mainVm.downloadJobs.collectAsStateWithLifecycle()
    val settings by mainVm.settings.collectAsStateWithLifecycle()
    val scraprMeta by mainVm.scraprMeta.collectAsStateWithLifecycle()
    val updateInfo by mainVm.updateInfo.collectAsStateWithLifecycle()
    val updateChecking by mainVm.updateChecking.collectAsStateWithLifecycle()
    val scraprInfo by mainVm.scraprInfo.collectAsStateWithLifecycle()
    val scraprBusy by mainVm.scraprBusy.collectAsStateWithLifecycle()
    val scraprMsg by mainVm.scraprMsg.collectAsStateWithLifecycle()
    val engineBusy by mainVm.engineBusy.collectAsStateWithLifecycle()
    val engineMsg by mainVm.engineMsg.collectAsStateWithLifecycle()
    val playerState by mainVm.playerState.collectAsStateWithLifecycle()
    val sheetTrack by mainVm.sheetTrack.collectAsStateWithLifecycle()

    // Account & Sync state
    val accountProfile by mainVm.accountProfile.collectAsStateWithLifecycle()
    val showLoginModal by mainVm.showLoginModal.collectAsStateWithLifecycle()
    val librarySubView by mainVm.librarySubView.collectAsStateWithLifecycle()
    val likedTracks by mainVm.likedTracks.collectAsStateWithLifecycle()
    val userPlaylists by mainVm.userPlaylists.collectAsStateWithLifecycle()
    val selectedPlaylist by mainVm.selectedPlaylist.collectAsStateWithLifecycle()
    val selectedPlaylistTracks by mainVm.selectedPlaylistTracks.collectAsStateWithLifecycle()
    val librarySyncing by mainVm.librarySyncing.collectAsStateWithLifecycle()
    val historySection by mainVm.historySection.collectAsStateWithLifecycle()
    val ytHistoryTracks by mainVm.ytHistoryTracks.collectAsStateWithLifecycle()
    val historySyncing by mainVm.historySyncing.collectAsStateWithLifecycle()

    // Spatial state
    val spatialSubTab by spatialVm.subTab.collectAsStateWithLifecycle()
    val spatialSettings by spatialVm.settings.collectAsStateWithLifecycle()
    val spatialFrame by spatialVm.frame.collectAsStateWithLifecycle()
    val previewPhase by spatialVm.previewPhase.collectAsStateWithLifecycle()
    val drawPathMode by spatialVm.drawPathMode.collectAsStateWithLifecycle()
    val draftPath by spatialVm.draftPath.collectAsStateWithLifecycle()
    val savedPresets by spatialVm.savedPresets.collectAsStateWithLifecycle()
    val presetNameInput by spatialVm.presetNameInput.collectAsStateWithLifecycle()
    val universalCaptureActive by spatialVm.universalCaptureActive.collectAsStateWithLifecycle()
    val globalEqActive by spatialVm.globalEqActive.collectAsStateWithLifecycle()
    val globalEqMessage by spatialVm.globalEqMessage.collectAsStateWithLifecycle()
    val floatingBubbleEnabled by spatialVm.floatingBubbleEnabled.collectAsStateWithLifecycle()
    val floatingLyricsEnabled by spatialVm.floatingLyricsEnabled.collectAsStateWithLifecycle()
    val floatingNotice by spatialVm.floatingNotice.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        merge(mainVm.toastMessage, spatialVm.toast).collect { msg ->
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(msg)
        }
    }

    BackHandler(enabled = drawerState.isOpen || currentTab != AppTab.HOME || (currentTab == AppTab.LIBRARY && librarySubView != LibrarySubView.OVERVIEW)) {
        when {
            drawerState.isOpen -> scope.launch { drawerState.close() }
            currentTab == AppTab.LIBRARY && librarySubView != LibrarySubView.OVERVIEW -> mainVm.closeLibrarySubView()
            else -> mainVm.selectTab(AppTab.HOME)
        }
    }

    val isSpatial = currentTab == AppTab.SPATIAL

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = YtCard,
                drawerContentColor = YtTextPrimary,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 24.dp),
                ) {
                    // Drawer header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(YtElevated)
                            .statusBarsPadding()
                            .clickable {
                                scope.launch { drawerState.close() }
                                if (accountProfile.isLoggedIn) {
                                    mainVm.selectTab(AppTab.LIBRARY)
                                    mainVm.openLibrarySubView(LibrarySubView.OVERVIEW)
                                } else {
                                    mainVm.openLoginModal()
                                }
                            }
                            .padding(horizontal = 18.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(YtOrange),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (accountProfile.isLoggedIn && accountProfile.avatarUrl.isNotBlank()) {
                                AsyncImage(
                                    model = accountProfile.avatarUrl,
                                    contentDescription = accountProfile.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp),
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (accountProfile.isLoggedIn) accountProfile.name.ifBlank { "Akun Google" } else "YMusic Lite",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = YtTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = if (accountProfile.isLoggedIn) accountProfile.handle.ifBlank { "Tersambung" } else "Ketuk untuk login Google",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (accountProfile.isLoggedIn) YtOrange else YtTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    HorizontalDivider(color = YtDivider)
                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "YouTube & Musik",
                        style = MaterialTheme.typography.labelMedium,
                        color = YtTextMuted,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                    )
                    PRIMARY_TABS.forEach { item ->
                        val selected = currentTab == item.tab
                        NavigationDrawerItem(
                            selected = selected,
                            onClick = {
                                scope.launch { drawerState.close() }
                                mainVm.selectTab(item.tab)
                                if (item.tab == AppTab.LIBRARY) {
                                    mainVm.openLibrarySubView(LibrarySubView.OVERVIEW)
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = null,
                                )
                            },
                            label = { Text(item.label, maxLines = 1) },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = YtOrangeSoft,
                                selectedIconColor = YtOrange,
                                selectedTextColor = YtOrange,
                                unselectedIconColor = YtTextSecondary,
                                unselectedTextColor = YtTextPrimary,
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 2.dp)
                                .testTag("drawer_${item.tab.route}"),
                        )
                    }

                    HorizontalDivider(color = YtDivider, modifier = Modifier.padding(vertical = 8.dp))

                    Text(
                        text = "Alat & Audio Studio",
                        style = MaterialTheme.typography.labelMedium,
                        color = YtTextMuted,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                    )
                    DRAWER_TOOL_TABS.forEach { item ->
                        val selected = currentTab == item.tab
                        NavigationDrawerItem(
                            selected = selected,
                            onClick = {
                                scope.launch { drawerState.close() }
                                mainVm.selectTab(item.tab)
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = null,
                                )
                            },
                            label = { Text(item.label, maxLines = 1) },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = YtOrangeSoft,
                                selectedIconColor = YtOrange,
                                selectedTextColor = YtOrange,
                                unselectedIconColor = YtTextSecondary,
                                unselectedTextColor = YtTextPrimary,
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 2.dp)
                                .testTag("drawer_${item.tab.route}"),
                        )
                    }
                }
            }
        },
    ) {
        Scaffold(
            containerColor = if (isSpatial) SpatialBg else YtBg,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    TopAppBar(
                        navigationIcon = {
                            IconButton(
                                onClick = { scope.launch { drawerState.open() } },
                                modifier = Modifier.testTag("topbar_drawer_btn"),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Menu",
                                    tint = if (isSpatial) TextPrimary else YtTextPrimary,
                                )
                            }
                        },
                        title = {
                            Text(
                                text = currentTab.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Medium,
                                color = if (isSpatial) TextPrimary else YtTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        actions = {
                            if (isSpatial) {
                                IconButton(
                                    onClick = { mainVm.selectTab(AppTab.HOME) },
                                    modifier = Modifier.testTag("topbar_spatial_home_btn"),
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Home,
                                        contentDescription = "Beranda",
                                        tint = SpatialGreen,
                                    )
                                }
                            } else {
                                IconButton(
                                    onClick = { mainVm.selectTab(AppTab.SEARCH) },
                                    modifier = Modifier.testTag("topbar_search_btn"),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Cari",
                                        tint = if (currentTab == AppTab.SEARCH) YtOrange else YtTextPrimary,
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = if (isSpatial) SpatialBg else YtCard,
                            titleContentColor = if (isSpatial) TextPrimary else YtTextPrimary,
                        ),
                    )

                    if (currentTab.isPrimaryTab) {
                        val selectedIndex = PRIMARY_TABS.indexOfFirst { it.tab == currentTab }.coerceAtLeast(0)
                        TabRow(
                            selectedTabIndex = selectedIndex,
                            containerColor = YtCard,
                            contentColor = YtOrange,
                            indicator = { tabPositions ->
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                                    height = 2.5.dp,
                                    color = YtOrange,
                                )
                            },
                            divider = { HorizontalDivider(color = YtDivider) },
                        ) {
                            PRIMARY_TABS.forEachIndexed { idx, item ->
                                val selected = idx == selectedIndex
                                Tab(
                                    selected = selected,
                                    onClick = {
                                        if (item.tab == AppTab.LIBRARY && selected) {
                                            mainVm.openLibrarySubView(LibrarySubView.OVERVIEW)
                                        } else {
                                            mainVm.selectTab(item.tab)
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = item.label,
                                            tint = if (selected) YtOrange else YtTextMuted,
                                            modifier = Modifier.size(23.dp),
                                        )
                                    },
                                    modifier = Modifier.testTag("nav_${item.tab.route}"),
                                )
                            }
                        }
                    } else if (!isSpatial) {
                        HorizontalDivider(color = YtDivider)
                    }
                }
            },
            bottomBar = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (playerState.mode == PlayerMode.YOUTUBE_IFRAME && !playerState.track?.videoId.isNullOrBlank()) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                            YouTubeEmbedOverlay(
                                videoId = playerState.track!!.videoId!!,
                                initialSeekSec = playerState.resumeSeekSec,
                                expanded = playerState.youtubeExpanded,
                                player = mainVm.player,
                                onSwitchToAudio = { mainVm.player.switchAudioVideoMode() },
                            )
                        }
                    }

                    MiniPlayerBar(
                        playerState = playerState,
                        onTogglePlayPause = { mainVm.player.toggle() },
                        onToggleAudioVideoMode = { mainVm.player.switchAudioVideoMode() },
                        onToggleYoutubeVideo = { mainVm.player.toggleYoutubeExpanded() },
                        onStop = { mainVm.player.stop() },
                    )
                }
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                when (currentTab) {
                    AppTab.HOME -> YouTubeFeedScreen(
                        tracks = homeFeed,
                        isLoading = homeLoading,
                        errorMessage = homeError,
                        onRetry = { mainVm.loadHomeFeed(force = true) },
                        onPlayTrack = mainVm::playTrack,
                        onMoreTrack = mainVm::openTrackSheet,
                        showChips = true,
                        selectedChip = homeChip,
                        onSelectChip = mainVm::selectHomeChip,
                    )

                    AppTab.TRENDING -> YouTubeFeedScreen(
                        tracks = trendingFeed,
                        isLoading = trendingLoading,
                        errorMessage = trendingError,
                        onRetry = mainVm::loadTrendingFeed,
                        onPlayTrack = mainVm::playTrack,
                        onMoreTrack = mainVm::openTrackSheet,
                    )

                    AppTab.RECOMMENDED -> YouTubeFeedScreen(
                        tracks = recommendedFeed,
                        isLoading = recommendedLoading,
                        errorMessage = recommendedError,
                        onRetry = mainVm.let { vm -> { vm.loadRecommendedFeed() } },
                        onPlayTrack = mainVm::playTrack,
                        onMoreTrack = mainVm::openTrackSheet,
                    )

                    AppTab.TOP_SONGS -> YouTubeFeedScreen(
                        tracks = topSongsFeed,
                        isLoading = topSongsLoading,
                        errorMessage = topSongsError,
                        onRetry = mainVm::loadTopSongsFeed,
                        onPlayTrack = mainVm::playTrack,
                        onMoreTrack = mainVm::openTrackSheet,
                    )

                    AppTab.LIBRARY -> LibraryScreen(
                        subView = librarySubView,
                        library = library,
                        accountProfile = accountProfile,
                        likedTracks = likedTracks,
                        userPlaylists = userPlaylists,
                        selectedPlaylist = selectedPlaylist,
                        selectedPlaylistTracks = selectedPlaylistTracks,
                        isSyncing = librarySyncing,
                        onSelectSubView = mainVm::openLibrarySubView,
                        onNavigateTab = mainVm::selectTab,
                        onSyncAccount = { mainVm.syncAccountLibrary(silent = false) },
                        onLoginGoogle = mainVm::openLoginModal,
                        onLogoutGoogle = mainVm::logoutAccount,
                        onOpenPlaylist = mainVm::openPlaylist,
                        onCloseSubView = mainVm::closeLibrarySubView,
                        onPlayTrack = mainVm::playTrack,
                        onMoreTrack = mainVm::openTrackSheet,
                    )

                    AppTab.SEARCH -> SearchScreen(
                        query = searchQuery,
                        results = searchResults,
                        isSearching = isSearching,
                        isDemo = isDemoSearch,
                        onQueryChange = mainVm::updateSearchQuery,
                        onPlayTrack = mainVm::playTrack,
                        onMoreTrack = mainVm::openTrackSheet,
                    )

                    AppTab.DOWNLOAD -> DownloadScreen(
                        urlInput = urlInput,
                        scrapeBusy = scrapeBusy,
                        lastScrape = lastScrape,
                        downloadJobs = downloadJobs,
                        onUrlChange = mainVm::updateUrlInput,
                        onScrape = mainVm::scrapeCurrentUrl,
                        onToast = mainVm::emitToast,
                        onDownloadItem = mainVm::downloadScrapedItem,
                        onSaveToLibrary = mainVm::saveLastScrapeToLibrary,
                        onPlayScrape = mainVm::playLastScrape,
                        onRemoveJob = mainVm::removeDownloadJob,
                        onClearFinishedJobs = mainVm::clearFinishedDownloads,
                    )

                    AppTab.SPATIAL -> SpatialScreen(
                        viewModel = spatialVm,
                        subTab = spatialSubTab,
                        settings = spatialSettings,
                        frame = spatialFrame,
                        previewPhase = previewPhase,
                        drawPathMode = drawPathMode,
                        draftPath = draftPath,
                        savedPresets = savedPresets,
                        presetNameInput = presetNameInput,
                        universalCaptureActive = universalCaptureActive,
                        globalEqActive = globalEqActive,
                        globalEqMessage = globalEqMessage,
                        floatingBubbleEnabled = floatingBubbleEnabled,
                        floatingLyricsEnabled = floatingLyricsEnabled,
                        floatingNotice = floatingNotice,
                    )

                    AppTab.HISTORY -> HistoryScreen(
                        history = history,
                        accountProfile = accountProfile,
                        selectedSection = historySection,
                        ytHistoryTracks = ytHistoryTracks,
                        isSyncing = historySyncing,
                        onSelectSection = mainVm::selectHistorySection,
                        onSyncYtHistory = { mainVm.syncYtMusicHistory(silent = false) },
                        onLoginGoogle = mainVm::openLoginModal,
                        onSelectHistory = mainVm::openHistoryItem,
                        onClearHistory = mainVm::clearHistory,
                        onPlayTrack = mainVm::playTrack,
                        onMoreTrack = mainVm::openTrackSheet,
                    )

                    AppTab.SETTINGS -> SettingsScreen(
                        accountProfile = accountProfile,
                        settings = settings,
                        scraprMeta = scraprMeta,
                        updateInfo = updateInfo,
                        updateChecking = updateChecking,
                        scraprInfo = scraprInfo,
                        scraprBusy = scraprBusy,
                        scraprMsg = scraprMsg,
                        engineBusy = engineBusy,
                        engineMsg = engineMsg,
                        onLoginGoogle = mainVm::openLoginModal,
                        onRefreshProfile = { mainVm.refreshAccountProfile(silent = false) },
                        onLogoutGoogle = mainVm::logoutAccount,
                        onCheckAppUpdate = mainVm::checkAppUpdate,
                        onOpenUrl = mainVm::openExternalUrl,
                        onToggleAutoCheckApp = mainVm::setAutoCheckAppUpdate,
                        onCheckScraprUpdate = mainVm::checkScraprUpdate,
                        onUpdateScrapr = mainVm::updateScraprFromOfficial,
                        onRevertScrapr = mainVm::revertScraprToBuiltin,
                        onToggleAutoUpdateEngine = mainVm::setAutoUpdateEngine,
                        onDownloadCustomEngine = mainVm::downloadCustomEngineConfig,
                        onRevertCustomEngine = mainVm::revertCustomEngine,
                        onDownloadFolderChange = mainVm::setDownloadFolder,
                        onYoutubeApiKeyChange = mainVm::setYoutubeApiKey,
                    )
                }
            }
        }
    }

    if (showLoginModal) {
        GoogleLoginDialog(
            onDismiss = mainVm::closeLoginModal,
            onCookiesCaptured = mainVm::onLoginCookiesCaptured,
            onError = mainVm::emitToast,
        )
    }

    sheetTrack?.let { track ->
        TrackOptionsBottomSheet(
            track = track,
            inLibrary = mainVm.isInLibrary(track.id),
            onDismiss = mainVm::closeTrackSheet,
            onPlay = { mainVm.playTrack(track) },
            onToggleLibrary = { mainVm.toggleLibrary(track) },
            onDownload = { format -> mainVm.enqueueDownload(track, format) },
        )
    }
}
