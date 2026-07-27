package dev.typetype.android.feature.library.playlist

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.typetype.android.R
import dev.typetype.android.toPlaybackQueueEntry
import dev.typetype.android.core.ui.components.LibraryFilterBar
import dev.typetype.android.core.ui.components.LibrarySortMode
import dev.typetype.android.core.ui.components.LocalAppSnackbarHost
import dev.typetype.android.core.ui.components.PlaylistVideoActionsSheet
import dev.typetype.android.core.ui.components.PlaylistVideoCard
import dev.typetype.android.domain.library.PlaylistVideo
import dev.typetype.android.feature.library.components.rememberVideoMetas
import dev.typetype.android.feature.library.LibrarySyncStatusBar
import dev.typetype.android.feature.menu.VideoMenuEvent
import dev.typetype.android.feature.menu.VideoMenuHandlerViewModel
import dev.typetype.android.feature.menu.blockVideoUrl
import dev.typetype.android.feature.menu.removeFromPlaylist
import dev.typetype.android.feature.menu.toggleWatchedUrl

@Composable
fun PlaylistRoute(
    onNavigateBack: () -> Unit,
    onPlayVideo: (videoUrl: String) -> Unit,
    onPlayQueue: (title: String, videos: List<PlaylistVideo>, shuffle: Boolean) -> Unit,
    onOpenChannel: (channelUrl: String) -> Unit = {},
    viewModel: PlaylistViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel, onNavigateBack) {
        viewModel.events.collect { event ->
            if (event == PlaylistDetailEvent.Deleted) onNavigateBack()
        }
    }
    PlaylistScreen(
        playlistId = state.playlistId,
        title = state.title,
        videos = state.videos,
        isLoading = state.isLoading,
        isRefreshing = state.isRefreshing,
        isReordering = state.isReordering,
        isMutationInFlight = state.isMutationInFlight,
        errorMessage = state.errorMessage,
        errorRequestId = state.errorRequestId,
        onNavigateBack = onNavigateBack,
        onPlayVideo = onPlayVideo,
        onPlayQueue = onPlayQueue,
        onOpenChannel = onOpenChannel,
        onRetry = viewModel::retry,
        onMoveVideo = viewModel::moveVideo,
        onRenamePlaylist = viewModel::renamePlaylist,
        onDeletePlaylist = viewModel::deletePlaylist,
    )
}

@Composable
private fun PlaylistScreen(
    playlistId: String,
    title: String,
    videos: List<PlaylistVideo>,
    isLoading: Boolean,
    isRefreshing: Boolean,
    isReordering: Boolean,
    isMutationInFlight: Boolean,
    errorMessage: String?,
    errorRequestId: String?,
    onNavigateBack: () -> Unit,
    onPlayVideo: (String) -> Unit,
    onPlayQueue: (title: String, videos: List<PlaylistVideo>, shuffle: Boolean) -> Unit,
    onOpenChannel: (String) -> Unit,
    onRetry: () -> Unit,
    onMoveVideo: (videoUrl: String, direction: Int) -> Unit,
    onRenamePlaylist: (String) -> Unit,
    onDeletePlaylist: () -> Unit,
) {
    var filter by rememberSaveable { mutableStateOf("") }
    var sort by rememberSaveable { mutableStateOf(LibrarySortMode.DefaultOrder) }
    val sortOptions = listOf(
        LibrarySortMode.DefaultOrder,
        LibrarySortMode.TitleAZ,
        LibrarySortMode.TitleZA,
    )
    val filtered = if (filter.isBlank()) {
        videos
    } else {
        val needle = filter.trim()
        videos.filter { it.title.contains(needle, ignoreCase = true) }
    }
    val visible = when (sort) {
        LibrarySortMode.TitleAZ -> filtered.sortedBy { it.title.lowercase() }
        LibrarySortMode.TitleZA -> filtered.sortedByDescending { it.title.lowercase() }
        else -> filtered.sortedBy { it.position }
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
    val context = LocalContext.current
    val shareChooserTitle = stringResource(R.string.video_menu_share_chooser)
    val serverBaseUrl = dev.typetype.android.core.ui.share.LocalServerBaseUrl.current
    var pendingMenu by remember { mutableStateOf<PlaylistVideo?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        PlaylistDetailTopBar(
            title = title,
            isMutationInFlight = isMutationInFlight,
            onNavigateBack = onNavigateBack,
            onRename = onRenamePlaylist,
            onDelete = onDeletePlaylist,
        )

        LibrarySyncStatusBar(
            isRefreshing = isRefreshing || isReordering || isMutationInFlight,
            lastSuccessfulSyncAtMillis = null,
            errorMessage = errorMessage,
            requestId = errorRequestId,
            pendingWriteCount = 0,
            failedWriteCount = 0,
            onRetry = onRetry,
        )

        if (videos.isNotEmpty()) {
            PlaylistPlaybackActions(
                enabled = !isLoading && !isRefreshing && !isReordering && !isMutationInFlight,
                onPlayAll = { onPlayQueue(title, videos.sortedBy { it.position }, false) },
                onShuffle = { onPlayQueue(title, videos, true) },
            )
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.state_loading),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return
        }

        if (videos.isNotEmpty()) {
            LibraryFilterBar(
                query = filter,
                onQueryChange = { filter = it },
                sortOptions = sortOptions,
                selectedSort = sort,
                onSortChange = { sort = it },
            )
        }

        if (visible.isEmpty()) {
            val message = if (filter.isBlank()) {
                stringResource(R.string.playlist_empty)
            } else {
                stringResource(R.string.library_filter_no_match, filter)
            }
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return
        }

        val urlsMissingInfo = visible
            .filter { it.channelAvatarUrl.isBlank() || it.channelName.isBlank() }
            .map { it.url }
        val metas = rememberVideoMetas(urlsMissingInfo)
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 360.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        ) {
            items(visible, key = { it.id }) { video ->
                PlaylistVideoCard(
                    video = video,
                    onClick = { onPlayVideo(video.url) },
                    onLongPress = { pendingMenu = video },
                    isWatched = video.url in watchedUrls,
                    meta = metas[video.url],
                    onChannelClick = onOpenChannel,
                    onMoreClick = { pendingMenu = video },
                )
            }
        }
    }

    pendingMenu?.let { video ->
        val orderedVideos = videos.sortedBy { it.position }
        val videoIndex = orderedVideos.indexOfFirst { it.url == video.url }
        val canReorder = filter.isBlank() && sort == LibrarySortMode.DefaultOrder && !isReordering
        PlaylistVideoActionsSheet(
            removeLabel = stringResource(R.string.playlist_action_remove_from_playlist, title),
            isWatched = video.url in watchedUrls,
            canMoveUp = canReorder && videoIndex > 0,
            canMoveDown = canReorder && videoIndex in 0 until orderedVideos.lastIndex,
            onMoveUp = if (filter.isBlank() && sort == LibrarySortMode.DefaultOrder) {
                { onMoveVideo(video.url, -1) }
            } else {
                null
            },
            onMoveDown = if (filter.isBlank() && sort == LibrarySortMode.DefaultOrder) {
                { onMoveVideo(video.url, 1) }
            } else {
                null
            },
            onPlayNext = { menuVm.playNext(video.toPlaybackQueueEntry()) },
            onAddToQueue = { menuVm.addToQueue(video.toPlaybackQueueEntry()) },
            onRemoveFromList = { menuVm.removeFromPlaylist(playlistId, title, video.url) },
            onToggleWatched = {
                menuVm.toggleWatchedUrl(
                    videoUrl = video.url,
                    title = video.title,
                    thumbnail = video.thumbnailUrl,
                    duration = video.durationSeconds,
                    isCurrentlyWatched = video.url in watchedUrls,
                )
            },
            onShare = {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(
                        Intent.EXTRA_TEXT,
                        dev.typetype.android.core.ui.share.buildShareUrl(serverBaseUrl, video.url),
                    )
                }
                context.startActivity(Intent.createChooser(intent, shareChooserTitle))
            },
            onBlockVideo = { menuVm.blockVideoUrl(video.url) },
            onDismiss = { pendingMenu = null },
        )
    }
}
