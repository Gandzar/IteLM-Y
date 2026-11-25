package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SpatialAudioOff
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.AccountProfile
import com.example.data.AppTab
import com.example.data.LibrarySubView
import com.example.data.Track
import com.example.data.YtPlaylist
import com.example.ui.components.EmptyStateView
import com.example.ui.components.YouTubeFeedRowItem
import com.example.ui.theme.YtBadgeScrim
import com.example.ui.theme.YtCard
import com.example.ui.theme.YtDivider
import com.example.ui.theme.YtElevated
import com.example.ui.theme.YtOrange
import com.example.ui.theme.YtTextMuted
import com.example.ui.theme.YtTextPrimary
import com.example.ui.theme.YtTextSecondary

@Composable
fun LibraryScreen(
    subView: LibrarySubView,
    library: List<Track>,
    accountProfile: AccountProfile,
    likedTracks: List<Track>,
    userPlaylists: List<YtPlaylist>,
    selectedPlaylist: YtPlaylist?,
    selectedPlaylistTracks: List<Track>,
    isSyncing: Boolean,
    onSelectSubView: (LibrarySubView) -> Unit,
    onNavigateTab: (AppTab) -> Unit,
    onSyncAccount: () -> Unit,
    onLoginGoogle: () -> Unit,
    onLogoutGoogle: () -> Unit,
    onOpenPlaylist: (YtPlaylist) -> Unit,
    onCloseSubView: () -> Unit,
    onPlayTrack: (Track) -> Unit,
    onMoreTrack: (Track) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (subView) {
        LibrarySubView.OVERVIEW -> LibraryOverview(
            libraryCount = library.size,
            likedCount = likedTracks.size,
            accountProfile = accountProfile,
            userPlaylists = userPlaylists,
            isSyncing = isSyncing,
            onSelectSubView = onSelectSubView,
            onNavigateTab = onNavigateTab,
            onSyncAccount = onSyncAccount,
            onLoginGoogle = onLoginGoogle,
            onLogoutGoogle = onLogoutGoogle,
            onOpenPlaylist = onOpenPlaylist,
            modifier = modifier,
        )

        LibrarySubView.LOCAL -> SubViewScaffold(
            title = "Library lokal (${library.size})",
            showRefresh = false,
            isSyncing = false,
            onBack = onCloseSubView,
            onRefresh = {},
            modifier = modifier,
        ) {
            if (library.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Outlined.LibraryMusic,
                    title = "Library lokal kosong",
                    subtitle = "Simpan lagu dari pencarian, beranda, atau downloader ke koleksi lokal.",
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    items(library, key = { it.id }) { track ->
                        YouTubeFeedRowItem(
                            track = track,
                            onPlay = { onPlayTrack(track) },
                            onMore = { onMoreTrack(track) },
                            modifier = Modifier.widthIn(max = 720.dp),
                        )
                    }
                }
            }
        }

        LibrarySubView.LIKED_MUSIC -> SubViewScaffold(
            title = "Video yang disukai",
            showRefresh = accountProfile.isLoggedIn,
            isSyncing = isSyncing,
            onBack = onCloseSubView,
            onRefresh = onSyncAccount,
            modifier = modifier,
        ) {
            when {
                !accountProfile.isLoggedIn -> LoginRequiredPlaceholder(
                    title = "Login untuk melihat Video yang disukai",
                    subtitle = "Masuk dengan akun Google untuk menyinkronkan lagu & video yang kamu sukai dari YouTube Music.",
                    onLoginGoogle = onLoginGoogle,
                )
                isSyncing && likedTracks.isEmpty() -> LoadingLibraryState()
                likedTracks.isEmpty() -> EmptyStateView(
                    icon = Icons.Outlined.FavoriteBorder,
                    title = "Belum ada lagu yang disukai",
                    subtitle = "Ketuk ikon refresh di atas untuk menyinkronkan Liked Music.",
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    items(likedTracks, key = { "liked_${it.id}" }) { track ->
                        YouTubeFeedRowItem(
                            track = track,
                            onPlay = { onPlayTrack(track) },
                            onMore = { onMoreTrack(track) },
                            modifier = Modifier.widthIn(max = 720.dp),
                        )
                    }
                }
            }
        }

        LibrarySubView.PLAYLISTS -> {
            val activePlaylist = selectedPlaylist
            val headerTitle = activePlaylist?.title ?: "Playlist (${userPlaylists.size})"
            SubViewScaffold(
                title = headerTitle,
                showRefresh = accountProfile.isLoggedIn && activePlaylist == null,
                isSyncing = isSyncing,
                onBack = onCloseSubView,
                onRefresh = onSyncAccount,
                modifier = modifier,
            ) {
                when {
                    !accountProfile.isLoggedIn -> LoginRequiredPlaceholder(
                        title = "Login untuk melihat Playlist",
                        subtitle = "Masuk dengan akun Google untuk mengakses playlist YouTube & YouTube Music kamu.",
                        onLoginGoogle = onLoginGoogle,
                    )
                    activePlaylist != null -> {
                        when {
                            isSyncing && selectedPlaylistTracks.isEmpty() -> LoadingLibraryState()
                            selectedPlaylistTracks.isEmpty() -> EmptyStateView(
                                icon = Icons.AutoMirrored.Filled.PlaylistPlay,
                                title = "Playlist kosong",
                                subtitle = "Tidak ada lagu yang dapat dimuat dari playlist ini.",
                            )
                            else -> LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                items(selectedPlaylistTracks, key = { "pl_${activePlaylist.id}_${it.id}" }) { track ->
                                    YouTubeFeedRowItem(
                                        track = track,
                                        onPlay = { onPlayTrack(track) },
                                        onMore = { onMoreTrack(track) },
                                        modifier = Modifier.widthIn(max = 720.dp),
                                    )
                                }
                            }
                        }
                    }
                    isSyncing && userPlaylists.isEmpty() -> LoadingLibraryState()
                    userPlaylists.isEmpty() -> EmptyStateView(
                        icon = Icons.AutoMirrored.Filled.PlaylistPlay,
                        title = "Playlist tidak ditemukan",
                        subtitle = "Ketuk ikon refresh di atas untuk mengambil playlist.",
                    )
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        items(userPlaylists, key = { it.browseId }) { playlist ->
                            YouTubePlaylistRowItem(
                                playlist = playlist,
                                onClick = { onOpenPlaylist(playlist) },
                                modifier = Modifier.widthIn(max = 720.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryOverview(
    libraryCount: Int,
    likedCount: Int,
    accountProfile: AccountProfile,
    userPlaylists: List<YtPlaylist>,
    isSyncing: Boolean,
    onSelectSubView: (LibrarySubView) -> Unit,
    onNavigateTab: (AppTab) -> Unit,
    onSyncAccount: () -> Unit,
    onLoginGoogle: () -> Unit,
    onLogoutGoogle: () -> Unit,
    onOpenPlaylist: (YtPlaylist) -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Profile Header
        item {
            Row(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth()
                    .clickable {
                        if (!accountProfile.isLoggedIn) onLoginGoogle()
                    }
                    .padding(horizontal = 18.dp, vertical = 14.dp)
                    .testTag("lib_account_header"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(YtElevated),
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
                            contentDescription = "Akun Google",
                            tint = if (accountProfile.isLoggedIn) YtOrange else YtTextMuted,
                            modifier = Modifier.size(48.dp),
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (accountProfile.isLoggedIn) {
                            accountProfile.name.ifBlank { "Akun Google" }
                        } else {
                            "Masuk ke akun Google"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = YtTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (accountProfile.isLoggedIn) {
                            accountProfile.handle.ifBlank { "YouTube Music tersinkron" }
                        } else {
                            "Sinkronkan playlist & video yang disukai"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = YtTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("lib_account_more_btn"),
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menu akun",
                            tint = YtTextMuted,
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.background(YtCard),
                    ) {
                        if (accountProfile.isLoggedIn) {
                            DropdownMenuItem(
                                text = { Text("Sinkronkan Library", color = YtTextPrimary) },
                                onClick = {
                                    menuExpanded = false
                                    onSyncAccount()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Logout", color = Color(0xFFD32F2F)) },
                                onClick = {
                                    menuExpanded = false
                                    onLogoutGoogle()
                                },
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("Login Google / YouTube", color = YtOrange) },
                                onClick = {
                                    menuExpanded = false
                                    onLoginGoogle()
                                },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Pengaturan", color = YtTextPrimary) },
                            onClick = {
                                menuExpanded = false
                                onNavigateTab(AppTab.SETTINGS)
                            },
                        )
                    }
                }
            }
            HorizontalDivider(color = YtDivider, modifier = Modifier.widthIn(max = 720.dp))
        }

        // Quick action menu items matching Screenshot 2
        item {
            Column(modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth()) {
                LibraryMenuRow(
                    icon = Icons.Outlined.AccountCircle,
                    label = if (libraryCount > 0) "Library lokal ($libraryCount)" else "Library lokal",
                    onClick = { onSelectSubView(LibrarySubView.LOCAL) },
                    testTag = "lib_menu_local",
                )
                LibraryMenuRow(
                    icon = Icons.Outlined.History,
                    label = "Riwayat tontonan",
                    onClick = { onNavigateTab(AppTab.HISTORY) },
                    testTag = "lib_menu_history",
                )
                LibraryMenuRow(
                    icon = Icons.Outlined.Download,
                    label = "All Downloader & Unduhan",
                    onClick = { onNavigateTab(AppTab.DOWNLOAD) },
                    testTag = "lib_menu_downloader",
                )
                LibraryMenuRow(
                    icon = Icons.Outlined.ThumbUp,
                    label = if (likedCount > 0) "Video yang disukai ($likedCount)" else "Video yang disukai",
                    onClick = { onSelectSubView(LibrarySubView.LIKED_MUSIC) },
                    testTag = "lib_menu_liked",
                )
                LibraryMenuRow(
                    icon = Icons.Outlined.SpatialAudioOff,
                    label = "Spatial 8D Studio",
                    onClick = { onNavigateTab(AppTab.SPATIAL) },
                    testTag = "lib_menu_spatial",
                )
                LibraryMenuRow(
                    icon = Icons.Outlined.Settings,
                    label = "Pengaturan",
                    onClick = { onNavigateTab(AppTab.SETTINGS) },
                    testTag = "lib_menu_settings",
                )
                HorizontalDivider(color = YtDivider, modifier = Modifier.padding(top = 4.dp))
            }
        }

        // Playlist section header with orange bar + "Lihat semua >"
        item {
            Row(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 8.dp, top = 12.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(18.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(YtOrange),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Playlist",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = YtTextPrimary,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = { onSelectSubView(LibrarySubView.PLAYLISTS) },
                    modifier = Modifier.testTag("lib_see_all_playlists"),
                ) {
                    Text(
                        text = "Lihat semua",
                        style = MaterialTheme.typography.bodyMedium,
                        color = YtTextSecondary,
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                        tint = YtTextSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

        if (!accountProfile.isLoggedIn) {
            item {
                Box(modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp)) {
                    Button(
                        onClick = onLoginGoogle,
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = YtOrange, contentColor = Color.White),
                        modifier = Modifier.fillMaxWidth().testTag("lib_login_cta_btn"),
                    ) {
                        Text("Login Google untuk Sinkronkan Playlist", fontWeight = FontWeight.Medium)
                    }
                }
            }
        } else if (isSyncing && userPlaylists.isEmpty()) {
            item {
                LoadingLibraryState()
            }
        } else if (userPlaylists.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .widthIn(max = 720.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Belum ada playlist tersinkron",
                        style = MaterialTheme.typography.bodyMedium,
                        color = YtTextSecondary,
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onSyncAccount) {
                        Text("Sinkronkan sekarang", color = YtOrange)
                    }
                }
            }
        } else {
            items(userPlaylists, key = { it.browseId }) { playlist ->
                YouTubePlaylistRowItem(
                    playlist = playlist,
                    onClick = { onOpenPlaylist(playlist) },
                    modifier = Modifier.widthIn(max = 720.dp),
                )
            }
        }
    }
}

@Composable
private fun LibraryMenuRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    testTag: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = YtTextPrimary.copy(alpha = 0.78f),
            modifier = Modifier.size(23.dp),
        )
        Spacer(Modifier.width(24.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = YtTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun YouTubePlaylistRowItem(
    playlist: YtPlaylist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 8.dp)
            .testTag("playlist_item_${playlist.id}"),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .width(152.dp)
                .height(86.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(YtElevated),
            contentAlignment = Alignment.Center,
        ) {
            if (playlist.thumbnail.isNotBlank()) {
                AsyncImage(
                    model = playlist.thumbnail,
                    contentDescription = playlist.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                    contentDescription = null,
                    tint = YtOrange,
                    modifier = Modifier.size(30.dp),
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(5.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(YtBadgeScrim)
                    .padding(horizontal = 5.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(13.dp),
                )
                Text(
                    text = playlist.trackCount.ifBlank { "Playlist" },
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = 2.dp),
        ) {
            Text(
                text = playlist.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = YtTextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (playlist.subtitle.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = playlist.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = YtTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        IconButton(
            onClick = onClick,
            modifier = Modifier.minimumInteractiveComponentSize(),
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Buka playlist",
                tint = YtTextMuted,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun SubViewScaffold(
    title: String,
    showRefresh: Boolean,
    isSyncing: Boolean,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.minimumInteractiveComponentSize().testTag("lib_subview_back"),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Kembali",
                    tint = YtTextPrimary,
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = YtTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (showRefresh) {
                IconButton(
                    onClick = onRefresh,
                    enabled = !isSyncing,
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("lib_sync_btn"),
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Sinkronkan",
                        tint = YtOrange,
                    )
                }
            }
        }
        HorizontalDivider(color = YtDivider, modifier = Modifier.widthIn(max = 720.dp))
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            content()
        }
    }
}

@Composable
private fun LoadingLibraryState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(color = YtOrange)
        Spacer(Modifier.height(12.dp))
        Text(
            "Menyinkronkan dari YouTube Music…",
            style = MaterialTheme.typography.bodySmall,
            color = YtTextSecondary,
        )
    }
}

@Composable
private fun LoginRequiredPlaceholder(
    title: String,
    subtitle: String,
    onLoginGoogle: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.LibraryMusic,
            contentDescription = null,
            tint = YtTextMuted,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(text = title, style = MaterialTheme.typography.titleMedium, color = YtTextPrimary)
        Spacer(Modifier.height(6.dp))
        Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = YtTextSecondary)
        Spacer(Modifier.height(18.dp))
        Button(
            onClick = onLoginGoogle,
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.buttonColors(containerColor = YtOrange, contentColor = Color.White),
            modifier = Modifier.testTag("lib_login_cta_btn"),
        ) {
            Text("Login Google / YouTube", fontWeight = FontWeight.Medium)
        }
    }
}
