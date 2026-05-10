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
import dev.typetype.android.R
import dev.typetype.android.core.ui.components.FullScreenLoader
import dev.typetype.android.core.ui.components.LibraryFilterBar
import dev.typetype.android.core.ui.components.LibrarySortMode
import dev.typetype.android.domain.library.HistoryItem
import dev.typetype.android.domain.library.Playlist
import dev.typetype.android.domain.library.PlaylistVideo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LibraryRoute(
    onPlayVideo: (videoUrl: String) -> Unit,
    onOpenPlaylist: (playlistId: String) -> Unit = {},
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LibraryScreen(
        state = state,
        onPlayVideo = onPlayVideo,
        onOpenPlaylist = onOpenPlaylist,
        onAction = viewModel::onAction,
    )
}

@Composable
fun LibraryScreen(
    state: LibraryState,
    onPlayVideo: (videoUrl: String) -> Unit,
    onOpenPlaylist: (playlistId: String) -> Unit,
    onAction: (LibraryAction) -> Unit,
) {
    val tabs = LibraryTab.entries
    var filter by rememberSaveable(state.selectedTab) { mutableStateOf("") }
    var sort by rememberSaveable(state.selectedTab) {
        mutableStateOf(defaultSortFor(state.selectedTab))
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
            )
            LibraryTab.Favorites -> FavoritesTab(
                items = sortPlaylistVideos(filterPlaylistVideos(state.favorites, filter), sort),
                filter = filter,
                onPlayVideo = onPlayVideo,
            )
            LibraryTab.WatchLater -> WatchLaterTab(
                items = sortPlaylistVideos(filterPlaylistVideos(state.watchLater, filter), sort),
                filter = filter,
                onPlayVideo = onPlayVideo,
            )
            LibraryTab.Playlists -> PlaylistsTab(
                playlists = sortPlaylists(filterPlaylists(state.playlists, filter), sort),
                filter = filter,
                onOpenPlaylist = onOpenPlaylist,
            )
        }
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
) {
    if (items.isEmpty()) {
        EmptyTab(emptyMessageFor(filter, "No watch history yet"))
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        items(items, key = { it.id }) { item ->
            HistoryRow(item = item, onClick = { onPlayVideo(item.url) })
        }
    }
}

@Composable
private fun HistoryRow(item: HistoryItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(120.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            AsyncImage(
                model = item.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = item.channelName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatDate(item.watchedAtMillis),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FavoritesTab(
    items: List<PlaylistVideo>,
    filter: String,
    onPlayVideo: (String) -> Unit,
) {
    PlaylistVideosList(
        items = items,
        emptyMessage = emptyMessageFor(filter, "No favorites yet"),
        onPlayVideo = onPlayVideo,
    )
}

@Composable
private fun WatchLaterTab(
    items: List<PlaylistVideo>,
    filter: String,
    onPlayVideo: (String) -> Unit,
) {
    PlaylistVideosList(
        items = items,
        emptyMessage = emptyMessageFor(filter, "Nothing in Watch Later"),
        onPlayVideo = onPlayVideo,
    )
}

@Composable
private fun emptyMessageFor(filter: String, default: String): String =
    if (filter.isBlank()) default else stringResource(R.string.library_filter_no_match, filter)

@Composable
private fun PlaylistVideosList(
    items: List<PlaylistVideo>,
    emptyMessage: String,
    onPlayVideo: (String) -> Unit,
) {
    if (items.isEmpty()) {
        EmptyTab(emptyMessage)
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        items(items, key = { it.id }) { video ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPlayVideo(video.url) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    AsyncImage(
                        model = video.thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (video.durationSeconds > 0) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = formatDuration(video.durationSeconds),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, secs)
    }
}

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
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(playlists, key = { it.id }) { playlist ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenPlaylist(playlist.id) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "${playlist.videos.size}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = playlist.name,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "${playlist.videos.size} videos",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
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
