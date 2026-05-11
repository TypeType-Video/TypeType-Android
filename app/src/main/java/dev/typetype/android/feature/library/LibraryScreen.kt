package dev.typetype.android.feature.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.typetype.android.R
import dev.typetype.android.core.ui.components.FullScreenLoader
import dev.typetype.android.core.ui.components.LibraryFilterBar

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
    var filter by rememberSaveable(state.selectedTab) { mutableStateOf("") }
    var sort by rememberSaveable(state.selectedTab) {
        mutableStateOf(defaultSortFor(state.selectedTab))
    }
    val (menuVm, watchedUrls) = rememberLibraryMenuHandler()

    Column(modifier = Modifier.fillMaxSize()) {
        LibraryTabs(
            selectedTab = state.selectedTab,
            onTabSelect = { onAction(LibraryAction.OnTabSelect(it)) },
        )

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
                emptyDefault = stringResource(R.string.library_empty_favorites),
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
                emptyDefault = stringResource(R.string.library_empty_watch_later),
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
