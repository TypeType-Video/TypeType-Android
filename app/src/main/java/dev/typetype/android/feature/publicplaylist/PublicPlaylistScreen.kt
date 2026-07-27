package dev.typetype.android.feature.publicplaylist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.typetype.android.R
import dev.typetype.android.core.ui.components.AnimatedError
import dev.typetype.android.core.ui.components.FullScreenLoader
import dev.typetype.android.core.ui.components.LazyPaginationFooter
import dev.typetype.android.core.ui.components.VideoCard
import dev.typetype.android.domain.feed.Video
import dev.typetype.android.feature.menu.rememberVideoMenuScope

@Composable
fun PublicPlaylistRoute(
    onNavigateBack: () -> Unit,
    onPlayVideo: (String) -> Unit,
    onPlayQueue: (String, List<Video>, Boolean) -> Unit,
    onOpenChannel: (String) -> Unit,
    viewModel: PublicPlaylistViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PublicPlaylistScreen(
        state = state,
        onNavigateBack = onNavigateBack,
        onPlayVideo = onPlayVideo,
        onPlayQueue = onPlayQueue,
        onOpenChannel = onOpenChannel,
        onAction = viewModel::onAction,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicPlaylistScreen(
    state: PublicPlaylistState,
    onNavigateBack: () -> Unit,
    onPlayVideo: (String) -> Unit,
    onPlayQueue: (String, List<Video>, Boolean) -> Unit,
    onOpenChannel: (String) -> Unit,
    onAction: (PublicPlaylistAction) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.public_playlist_title)) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.settings_back),
                    )
                }
            },
        )
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            when {
                state.isLoading && state.playlist == null -> FullScreenLoader()
                state.errorMessage != null && state.playlist == null -> AnimatedError(
                    message = state.errorMessage,
                    requestId = state.errorRequestId,
                    onRetry = { onAction(PublicPlaylistAction.OnRetry) },
                )
                state.playlist != null -> PublicPlaylistContent(
                    state = state,
                    onPlayVideo = onPlayVideo,
                    onPlayQueue = onPlayQueue,
                    onOpenChannel = onOpenChannel,
                    onLoadMore = { onAction(PublicPlaylistAction.OnLoadMore) },
                    onToggleSaved = { onAction(PublicPlaylistAction.OnToggleSaved) },
                )
            }
        }
    }
}

@Composable
private fun PublicPlaylistContent(
    state: PublicPlaylistState,
    onPlayVideo: (String) -> Unit,
    onPlayQueue: (String, List<Video>, Boolean) -> Unit,
    onOpenChannel: (String) -> Unit,
    onLoadMore: () -> Unit,
    onToggleSaved: () -> Unit,
) {
    val playlist = requireNotNull(state.playlist)
    val menuScope = rememberVideoMenuScope(onOpenChannel)
    val videos = state.videos.filterNot(menuScope::isHidden)
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 300.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }, key = "playlist-header") {
            PublicPlaylistHeader(
                playlist = playlist,
                loadedCount = videos.size,
                hasMore = state.nextPage != null,
                canSave = state.canSave,
                isSaved = state.savedItemId != null,
                saveInFlight = state.saveInFlight,
                saveErrorMessage = state.saveErrorMessage,
                onPlay = { onPlayQueue(playlist.title, videos, false) },
                onShuffle = { onPlayQueue(playlist.title, videos, true) },
                onToggleSaved = onToggleSaved,
            )
        }
        items(videos, key = { it.url }, contentType = { "public-playlist-video" }) { video ->
            VideoCard(
                video = video,
                onClick = { onPlayVideo(video.url) },
                onChannelClick = { onOpenChannel(video.uploaderUrl) },
                onMenuAction = { action -> menuScope.onAction(action, video) },
                menuItemState = menuScope.stateFor(video),
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
        if (videos.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = stringResource(R.string.public_playlist_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                )
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }, key = "playlist-pagination") {
            PublicPlaylistPagination(
                cursor = state.nextPage,
                loading = state.isLoadingMore,
                failed = state.loadMoreError,
                onLoadMore = onLoadMore,
            )
        }
    }
}

@Composable
private fun PublicPlaylistPagination(
    cursor: String?,
    loading: Boolean,
    failed: Boolean,
    onLoadMore: () -> Unit,
) {
    LazyPaginationFooter(
        continuationKey = cursor,
        isLoading = loading,
        hasError = failed,
        onLoadMore = onLoadMore,
    )
}
