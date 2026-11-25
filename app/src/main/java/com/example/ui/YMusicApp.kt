package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SpatialAudioOff
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SpatialAudioOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppTab
import com.example.player.PlayerMode
import com.example.ui.components.MiniPlayerBar
import com.example.ui.components.TrackOptionsBottomSheet
import com.example.ui.components.YouTubeEmbedOverlay
import com.example.ui.screens.DownloadScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SpatialScreen
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.BgElevated
import com.example.ui.theme.BgPrimary
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import kotlinx.coroutines.flow.merge

private data class NavTabMeta(
    val tab: AppTab,
    val label: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

private val NAV_TABS = listOf(
    NavTabMeta(AppTab.DOWNLOAD, "Download", "Downloader", Icons.Filled.Download, Icons.Outlined.Download),
    NavTabMeta(AppTab.SEARCH, "Search", "Search", Icons.Filled.Search, Icons.Outlined.Search),
    NavTabMeta(AppTab.SPATIAL, "8D", "Spatial 8D", Icons.Filled.SpatialAudioOff, Icons.Outlined.SpatialAudioOff),
    NavTabMeta(AppTab.LIBRARY, "Library", "Library", Icons.Filled.LibraryMusic, Icons.Outlined.LibraryMusic),
    NavTabMeta(AppTab.HISTORY, "Riwayat", "Riwayat", Icons.Filled.History, Icons.Outlined.History),
    NavTabMeta(AppTab.SETTINGS, "Settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YMusicApp(
    mainVm: MainViewModel = viewModel(),
    spatialVm: SpatialViewModel = viewModel(),
) {
    val currentTab by mainVm.currentTab.collectAsStateWithLifecycle()
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

    LaunchedEffect(Unit) {
        merge(mainVm.toastMessage, spatialVm.toast).collect { msg ->
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(msg)
        }
    }

    BackHandler(enabled = currentTab != AppTab.DOWNLOAD) {
        mainVm.selectTab(AppTab.DOWNLOAD)
    }

    val activeMeta = NAV_TABS.first { it.tab == currentTab }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 720.dp

        Scaffold(
            containerColor = BgPrimary,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                if (currentTab != AppTab.SPATIAL) {
                    Column {
                        TopAppBar(
                            title = {
                                Text(
                                    text = activeMeta.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = BgPrimary,
                                titleContentColor = TextPrimary,
                            ),
                        )
                        HorizontalDivider(color = BorderSubtle)
                    }
                }
            },
            bottomBar = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (playerState.mode == PlayerMode.YOUTUBE_IFRAME && !playerState.track?.videoId.isNullOrBlank()) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                            YouTubeEmbedOverlay(
                                videoId = playerState.track!!.videoId!!,
                                expanded = playerState.youtubeExpanded,
                                player = mainVm.player,
                            )
                        }
                    }

                    MiniPlayerBar(
                        playerState = playerState,
                        onTogglePlayPause = { mainVm.player.toggle() },
                        onToggleYoutubeVideo = { mainVm.player.toggleYoutubeExpanded() },
                        onStop = { mainVm.player.stop() },
                    )

                    if (!useRail) {
                        HorizontalDivider(color = BorderSubtle)
                        NavigationBar(
                            containerColor = BgElevated,
                            contentColor = TextPrimary,
                        ) {
                            NAV_TABS.forEach { item ->
                                val selected = item.tab == currentTab
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { mainVm.selectTab(item.tab) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = item.label,
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = item.label,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    },
                                    alwaysShowLabel = true,
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = AccentGreen,
                                        selectedTextColor = AccentGreen,
                                        indicatorColor = AccentGreen.copy(alpha = 0.16f),
                                        unselectedIconColor = TextMuted,
                                        unselectedTextColor = TextMuted,
                                    ),
                                    modifier = Modifier.testTag("nav_${item.tab.route}"),
                                )
                            }
                        }
                    }
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = BgElevated,
                        contentColor = TextPrimary,
                    ) {
                        NAV_TABS.forEach { item ->
                            val selected = item.tab == currentTab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { mainVm.selectTab(item.tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label,
                                    )
                                },
                                label = { Text(item.label, maxLines = 1) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = AccentGreen,
                                    selectedTextColor = AccentGreen,
                                    indicatorColor = AccentGreen.copy(alpha = 0.16f),
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted,
                                ),
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    when (currentTab) {
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

                        AppTab.SEARCH -> SearchScreen(
                            query = searchQuery,
                            results = searchResults,
                            isSearching = isSearching,
                            isDemo = isDemoSearch,
                            onQueryChange = mainVm::updateSearchQuery,
                            onPlayTrack = mainVm::playTrack,
                            onMoreTrack = mainVm::openTrackSheet,
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

                        AppTab.LIBRARY -> LibraryScreen(
                            library = library,
                            onPlayTrack = mainVm::playTrack,
                            onMoreTrack = mainVm::openTrackSheet,
                        )

                        AppTab.HISTORY -> HistoryScreen(
                            history = history,
                            onSelectHistory = mainVm::openHistoryItem,
                            onClearHistory = mainVm::clearHistory,
                        )

                        AppTab.SETTINGS -> SettingsScreen(
                            settings = settings,
                            scraprMeta = scraprMeta,
                            updateInfo = updateInfo,
                            updateChecking = updateChecking,
                            scraprInfo = scraprInfo,
                            scraprBusy = scraprBusy,
                            scraprMsg = scraprMsg,
                            engineBusy = engineBusy,
                            engineMsg = engineMsg,
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
