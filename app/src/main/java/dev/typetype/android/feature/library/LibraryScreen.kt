package dev.typetype.android.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import dev.typetype.android.R
import dev.typetype.android.core.ui.components.FullScreenLoader
import dev.typetype.android.core.ui.components.LibraryFilterBar
import dev.typetype.android.core.ui.components.LibrarySortMode
import dev.typetype.android.core.ui.components.PlaylistVideoActionsSheet
import dev.typetype.android.core.ui.components.PlaylistVideoCard
import dev.typetype.android.domain.library.HistoryItem
import dev.typetype.android.domain.library.Playlist
import dev.typetype.android.domain.library.PlaylistVideo
import dev.typetype.android.feature.library.components.rememberVideoMetas
import dev.typetype.android.feature.menu.VideoMenuEvent
import dev.typetype.android.feature.menu.VideoMenuHandlerViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.LaunchedEffect
import dev.typetype.android.core.ui.components.LocalAppSnackbarHost

@Composable
fun LibraryRoute(
    onPlayVideo: (videoUrl: String) -> Unit,
    onOpenPlaylist: (playlistId: String) -> Unit = {},
    onOpenChannel: (channelUrl: String) -> Unit = {},
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LibraryScreen(
        state = state,
        onPlayVideo = onPlayVideo,
        onOpenPlaylist = onOpenPlaylist,
        onOpenChannel = onOpenChannel,
        onAction = viewModel::onAction,
    )
}

@Composable
fun LibraryScreen(
    state: LibraryState,
    onPlayVideo: (videoUrl: String) -> Unit,
    onOpenPlaylist: (playlistId: String) -> Unit,
    onOpenChannel: (channelUrl: String) -> Unit,
    onAction: (LibraryAction) -> Unit,
) {
    val tabs = LibraryTab.entries
    var filter by rememberSaveable(state.selectedTab) { mutableStateOf("") }
    var sort by rememberSaveable(state.selectedTab) {
        mutableStateOf(defaultSortFor(state.selectedTab))
    }

    val menuVm: VideoMenuHandlerViewModel = hiltViewModel()
    val watchedUrls by menuVm.watchedUrls.collectAsStateWithLifecycle()
    val snackbarHost = LocalAppSnackbarHost.current
    LaunchedEffect(menuVm, snackbarHost) {
        if (snackbarHost == null) return@LaunchedEffect
        menuVm.events.collect { event ->
            when (event) {
                is VideoMenuEvent.Snackbar -> snackbarHost.showSnackbar(
                    event.message,
                    duration = SnackbarDuration.Short,
                )
            }
        }
    }
    Column(modifier = Modifier.fillMaxSize()) {
        SecondaryScrollableTabRow(
            selectedTabIndex = tabs.indexOf(state.selectedTab),
            containerColor = MaterialTheme.colorScheme.background,
            edgePadding = 0.dp,
        ) {
            tabs.forEach { tab ->
                Tab(
                    selected = state.selectedTab == tab,
                    onClick = { onAction(LibraryAction.OnTabSelect(tab)) },
                    text = {
                        Text(
                            text = when (tab) {
                                LibraryTab.History -> "History"
                                LibraryTab.Favorites -> "Favorites"
                                LibraryTab.WatchLater -> "Watch Later"
                                LibraryTab.Playlists -> "Playlists"
                            },
                        )
                    },
                )
            }
        }

        if (state.isLoading) {
            FullScreenLoader()
            return
        }

        LibraryFilterBar(
            query = filter,
            onQueryChange = { filter = it },
            sortOptions = sortOptionsFor(state.selectedTab),
            selectedSort = sort,
            onSortChange = { sort = it },
        )

        when (state.selectedTab) {
            LibraryTab.History -> HistoryTab(
                items = sortHistory(filterHistory(state.history, filter), sort),
                filter = filter,
                onPlayVideo = onPlayVideo,
                onOpenChannel = onOpenChannel,
            )
            LibraryTab.Favorites -> PlaylistContextTab(
                items = sortPlaylistVideos(filterPlaylistVideos(state.favorites, filter), sort),
                filter = filter,
                emptyDefault = "No favorites yet",
                watchedUrls = watchedUrls,
                onPlayVideo = onPlayVideo,
                onOpenChannel = onOpenChannel,
                buildRemoveLabel = { stringResource(R.string.playlist_action_remove_from_favorites) },
                onRemove = { video -> menuVm.removeFavoriteUrl(video.url) },
                onToggleWatched = { video, isWatched ->
                    menuVm.toggleWatchedUrl(
                        videoUrl = video.url,
                        title = video.title,
                        thumbnail = video.thumbnailUrl,
                        duration = video.durationSeconds,
                        isCurrentlyWatched = isWatched,
                    )
                },
                onBlockVideo = { video -> menuVm.blockVideoUrl(video.url) },
            )
            LibraryTab.WatchLater -> PlaylistContextTab(
                items = sortPlaylistVideos(filterPlaylistVideos(state.watchLater, filter), sort),
                filter = filter,
                emptyDefault = "Nothing in Watch Later",
                watchedUrls = watchedUrls,
                onPlayVideo = onPlayVideo,
                onOpenChannel = onOpenChannel,
                buildRemoveLabel = { stringResource(R.string.playlist_action_remove_from_watch_later) },
                onRemove = { video -> menuVm.removeWatchLaterUrl(video.url) },
                onToggleWatched = { video, isWatched ->
                    menuVm.toggleWatchedUrl(
                        videoUrl = video.url,
                        title = video.title,
                        thumbnail = video.thumbnailUrl,
                        duration = video.durationSeconds,
                        isCurrentlyWatched = isWatched,
                    )
                },
                onBlockVideo = { video -> menuVm.blockVideoUrl(video.url) },
            )
            LibraryTab.Playlists -> PlaylistsTab(
                playlists = sortPlaylists(filterPlaylists(state.playlists, filter), sort),
                filter = filter,
                onOpenPlaylist = onOpenPlaylist,
            )
        }
    }
}

@Composable
private fun PlaylistContextTab(
    items: List<PlaylistVideo>,
    filter: String,
    emptyDefault: String,
    watchedUrls: Set<String>,
    onPlayVideo: (String) -> Unit,
    onOpenChannel: (String) -> Unit,
    buildRemoveLabel: @Composable (PlaylistVideo) -> String,
    onRemove: (PlaylistVideo) -> Unit,
    onToggleWatched: (PlaylistVideo, Boolean) -> Unit,
    onBlockVideo: (PlaylistVideo) -> Unit,
) {
    if (items.isEmpty()) {
        EmptyTab(emptyMessageFor(filter, emptyDefault))
        return
    }
    val context = LocalContext.current
    val shareChooserTitle = stringResource(R.string.video_menu_share_chooser)
    var pendingMenu by remember { mutableStateOf<PlaylistVideo?>(null) }
    val urlsMissingInfo = items
        .filter { it.channelAvatarUrl.isBlank() || it.channelName.isBlank() }
        .map { it.url }
    val metas = rememberVideoMetas(urlsMissingInfo)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 4.dp),
    ) {
        items(items, key = { it.id }) { video ->
            PlaylistVideoCard(
                video = video,
                onClick = { onPlayVideo(video.url) },
                onLongPress = { pendingMenu = video },
                isWatched = video.url in watchedUrls,
                meta = metas[video.url],
                onChannelClick = onOpenChannel,
            )
        }
    }

    pendingMenu?.let { video ->
        PlaylistVideoActionsSheet(
            removeLabel = buildRemoveLabel(video),
            isWatched = video.url in watchedUrls,
            onRemoveFromList = { onRemove(video) },
            onToggleWatched = { onToggleWatched(video, video.url in watchedUrls) },
            onShare = {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, video.url)
                }
                context.startActivity(Intent.createChooser(intent, shareChooserTitle))
            },
            onBlockVideo = { onBlockVideo(video) },
            onDismiss = { pendingMenu = null },
        )
    }
}

private fun defaultSortFor(tab: LibraryTab): LibrarySortMode = when (tab) {
    LibraryTab.History -> LibrarySortMode.RecentFirst
    LibraryTab.Favorites -> LibrarySortMode.RecentFirst
    LibraryTab.WatchLater -> LibrarySortMode.RecentFirst
    LibraryTab.Playlists -> LibrarySortMode.RecentFirst
}

private fun sortOptionsFor(tab: LibraryTab): List<LibrarySortMode> = when (tab) {
    LibraryTab.History,
    LibraryTab.Favorites,
    LibraryTab.WatchLater -> listOf(
        LibrarySortMode.RecentFirst,
        LibrarySortMode.OldestFirst,
        LibrarySortMode.TitleAZ,
        LibrarySortMode.TitleZA,
    )
    LibraryTab.Playlists -> listOf(
        LibrarySortMode.RecentFirst,
        LibrarySortMode.OldestFirst,
        LibrarySortMode.NameAZ,
        LibrarySortMode.NameZA,
    )
}

private fun sortHistory(items: List<HistoryItem>, mode: LibrarySortMode): List<HistoryItem> = when (mode) {
    LibrarySortMode.OldestFirst -> items.sortedBy { it.watchedAtMillis }
    LibrarySortMode.TitleAZ -> items.sortedBy { it.title.lowercase() }
    LibrarySortMode.TitleZA -> items.sortedByDescending { it.title.lowercase() }
    else -> items.sortedByDescending { it.watchedAtMillis }
}

private fun sortPlaylistVideos(
    items: List<PlaylistVideo>,
    mode: LibrarySortMode,
): List<PlaylistVideo> = when (mode) {
    LibrarySortMode.OldestFirst -> items.sortedBy { it.position }
    LibrarySortMode.TitleAZ -> items.sortedBy { it.title.lowercase() }
    LibrarySortMode.TitleZA -> items.sortedByDescending { it.title.lowercase() }
    else -> items.sortedByDescending { it.position }
}

private fun sortPlaylists(items: List<Playlist>, mode: LibrarySortMode): List<Playlist> = when (mode) {
    LibrarySortMode.OldestFirst -> items.sortedBy { it.createdAtMillis }
    LibrarySortMode.NameAZ -> items.sortedBy { it.name.lowercase() }
    LibrarySortMode.NameZA -> items.sortedByDescending { it.name.lowercase() }
    else -> items.sortedByDescending { it.createdAtMillis }
}

private fun matchesFilter(text: String, filter: String): Boolean =
    filter.isBlank() || text.contains(filter.trim(), ignoreCase = true)

private fun filterHistory(items: List<HistoryItem>, filter: String): List<HistoryItem> =
    if (filter.isBlank()) items
    else items.filter { matchesFilter(it.title, filter) || matchesFilter(it.channelName, filter) }

private fun filterPlaylistVideos(items: List<PlaylistVideo>, filter: String): List<PlaylistVideo> =
    if (filter.isBlank()) items
    else items.filter { matchesFilter(it.title, filter) }

private fun filterPlaylists(items: List<Playlist>, filter: String): List<Playlist> =
    if (filter.isBlank()) items
    else items.filter { matchesFilter(it.name, filter) }

@Composable
private fun HistoryTab(
    items: List<HistoryItem>,
    filter: String,
    onPlayVideo: (String) -> Unit,
    onOpenChannel: (String) -> Unit,
) {
    if (items.isEmpty()) {
        EmptyTab(emptyMessageFor(filter, "No watch history yet"))
        return
    }
    val urlsMissingInfo = items
        .filter { it.channelAvatarUrl.isBlank() }
        .map { it.url }
    val metas = rememberVideoMetas(urlsMissingInfo)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 4.dp),
    ) {
        items(items, key = { it.id }) { item ->
            HistoryRow(
                item = item,
                meta = metas[item.url],
                onClick = { onPlayVideo(item.url) },
                onOpenChannel = onOpenChannel,
            )
        }
    }
}

@Composable
private fun HistoryRow(
    item: HistoryItem,
    meta: dev.typetype.android.domain.library.VideoMeta?,
    onClick: () -> Unit,
    onOpenChannel: (String) -> Unit,
) {
    val channelUrl = item.channelUrl.takeIf { it.isNotBlank() }
        ?: meta?.channelUrl?.takeIf { it.isNotBlank() }
    val avatarUrl = item.channelAvatarUrl.takeIf { it.isNotBlank() }
        ?: meta?.channelAvatarUrl?.takeIf { it.isNotBlank() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .width(160.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            AsyncImage(
                model = item.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            if (item.durationSeconds > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.85f))
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                ) {
                    Text(
                        text = formatVideoDuration(item.durationSeconds),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (avatarUrl != null) {
                    val avatarModifier = Modifier
                        .size(22.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .let {
                            if (channelUrl != null) it.clickable { onOpenChannel(channelUrl) } else it
                        }
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = avatarModifier,
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = item.channelName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = if (channelUrl != null) Modifier.clickable { onOpenChannel(channelUrl) } else Modifier,
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = formatDate(item.watchedAtMillis),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outlineVariant,
            )
        }
    }
}

private fun formatVideoDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

@Composable
private fun emptyMessageFor(filter: String, default: String): String =
    if (filter.isBlank()) default else stringResource(R.string.library_filter_no_match, filter)

@Composable
private fun PlaylistsTab(
    playlists: List<Playlist>,
    filter: String,
    onOpenPlaylist: (playlistId: String) -> Unit,
) {
    if (playlists.isEmpty()) {
        EmptyTab(emptyMessageFor(filter, "No playlists yet"))
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(playlists, key = { it.id }) { playlist ->
            PlaylistListCard(
                playlist = playlist,
                onClick = { onOpenPlaylist(playlist.id) },
            )
        }
    }
}

@Composable
private fun PlaylistListCard(playlist: Playlist, onClick: () -> Unit) {
    val countLabel = if (playlist.videos.size == 1) {
        stringResource(R.string.playlist_card_single_video)
    } else {
        stringResource(R.string.playlist_card_video_count, playlist.videos.size)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            val cover = playlist.videos.firstOrNull()?.thumbnailUrl
            if (cover != null) {
                AsyncImage(
                    model = cover,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.85f))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text(
                    text = countLabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = playlist.name,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = countLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyTab(message: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun formatDate(millis: Long): String =
    SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(millis))
