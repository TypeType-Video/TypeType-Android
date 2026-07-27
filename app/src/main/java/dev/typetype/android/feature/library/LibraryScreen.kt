package dev.typetype.android.feature.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.PagingData
import dev.typetype.android.R
import dev.typetype.android.toPlaybackQueueEntry
import dev.typetype.android.core.ui.components.FullScreenLoader
import dev.typetype.android.core.ui.components.LibraryFilterBar
import dev.typetype.android.core.ui.components.LibrarySortMode
import dev.typetype.android.domain.library.HistoryItem
import dev.typetype.android.feature.menu.blockVideoUrl
import dev.typetype.android.feature.menu.removeFavoriteUrl
import dev.typetype.android.feature.menu.removeWatchLaterUrl
import dev.typetype.android.feature.menu.toggleWatchedUrl
import kotlinx.coroutines.flow.Flow

@Composable
fun LibraryRoute(
    onPlayVideo: (videoUrl: String) -> Unit,
    onOpenPlaylist: (playlistId: String) -> Unit = {},
    onOpenPublicPlaylist: (playlistUrl: String) -> Unit = {},
    onOpenChannel: (channelUrl: String) -> Unit = {},
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LibraryScreen(
        state = state,
        historyPagingData = viewModel.historyPagingData,
        onPlayVideo = onPlayVideo,
        onOpenPlaylist = onOpenPlaylist,
        onOpenPublicPlaylist = onOpenPublicPlaylist,
        onOpenChannel = onOpenChannel,
        onAction = viewModel::onAction,
        onHistoryQueryChange = viewModel::updateHistoryQuery,
    )
}

@Composable
fun LibraryScreen(
    state: LibraryState,
    historyPagingData: Flow<PagingData<HistoryItem>>,
    onPlayVideo: (videoUrl: String) -> Unit,
    onOpenPlaylist: (playlistId: String) -> Unit,
    onOpenPublicPlaylist: (playlistUrl: String) -> Unit,
    onOpenChannel: (channelUrl: String) -> Unit,
    onAction: (LibraryAction) -> Unit,
    onHistoryQueryChange: (String, LibrarySortMode) -> Unit,
) {
    var filter by rememberSaveable(state.selectedTab) { mutableStateOf("") }
    var sort by rememberSaveable(state.selectedTab) {
        mutableStateOf(defaultSortFor(state.selectedTab))
    }
    val (menuVm, watchedUrls) = rememberLibraryMenuHandler()
    LaunchedEffect(state.selectedTab, filter, sort) {
        if (state.selectedTab == LibraryTab.History) {
            onHistoryQueryChange(filter, sort)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LibraryTabs(
            selectedTab = state.selectedTab,
            onTabSelect = { onAction(LibraryAction.OnTabSelect(it)) },
        )

        if (state.shouldShowInitialLoader()) {
            FullScreenLoader()
            return
        }

        LibrarySyncStatusBar(
            isRefreshing = state.isLoading,
            lastSuccessfulSyncAtMillis = state.lastSuccessfulSyncAtMillis,
            errorMessage = state.errorMessage,
            requestId = state.syncRequestId,
            pendingWriteCount = state.pendingWriteCount,
            failedWriteCount = state.failedWriteCount,
            onRetry = { onAction(LibraryAction.OnRetry) },
        )

        LibraryFilterBar(
            query = filter,
            onQueryChange = { filter = it },
            sortOptions = sortOptionsFor(state.selectedTab),
            selectedSort = sort,
            onSortChange = { sort = it },
        )

        when (state.selectedTab) {
            LibraryTab.History -> HistoryTab(
                pagingData = historyPagingData,
                filter = filter,
                isRefreshing = state.isLoading,
                isLoadingMore = state.isLoadingMoreHistory,
                hasMore = state.historyHasMore,
                onPlayVideo = onPlayVideo,
                onOpenChannel = onOpenChannel,
                onPlayNext = { menuVm.playNext(it.toPlaybackQueueEntry()) },
                onAddToQueue = { menuVm.addToQueue(it.toPlaybackQueueEntry()) },
                onLoadMore = { onAction(LibraryAction.OnLoadMoreHistory) },
            )
            LibraryTab.Favorites -> PlaylistContextTab(
                items = sortPlaylistVideos(filterPlaylistVideos(state.favorites, filter), sort),
                filter = filter,
                emptyDefault = stringResource(R.string.library_empty_favorites),
                watchedUrls = watchedUrls,
                onPlayVideo = onPlayVideo,
                onOpenChannel = onOpenChannel,
                onPlayNext = { menuVm.playNext(it.toPlaybackQueueEntry()) },
                onAddToQueue = { menuVm.addToQueue(it.toPlaybackQueueEntry()) },
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
                onPlayNext = { menuVm.playNext(it.toPlaybackQueueEntry()) },
                onAddToQueue = { menuVm.addToQueue(it.toPlaybackQueueEntry()) },
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
                isMutationInFlight = state.isPlaylistMutationInFlight,
                onOpenPlaylist = onOpenPlaylist,
                onCreatePlaylist = { onAction(LibraryAction.OnCreatePlaylist(it)) },
                onRenamePlaylist = { playlistId, name ->
                    onAction(LibraryAction.OnRenamePlaylist(playlistId, name))
                },
                onDeletePlaylist = { onAction(LibraryAction.OnDeletePlaylist(it)) },
            )
            LibraryTab.SavedPlaylists -> SavedPlaylistsTab(
                playlists = sortSavedPlaylists(
                    filterSavedPlaylists(state.savedPlaylists, filter),
                    sort,
                ),
                filter = filter,
                canSave = state.canSavePublicPlaylists,
                isMutationInFlight = state.isSavedPlaylistMutationInFlight,
                onOpenPlaylist = onOpenPublicPlaylist,
                onRemovePlaylist = {
                    onAction(LibraryAction.OnRemoveSavedPlaylist(it))
                },
            )
        }
    }
}
