package dev.typetype.android.feature.podcast

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
fun PodcastRoute(
    onNavigateBack: () -> Unit,
    onPlayVideo: (String) -> Unit,
    onPlayQueue: (String, List<Video>, Boolean) -> Unit,
    onOpenChannel: (String) -> Unit,
    viewModel: PodcastViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PodcastScreen(
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
fun PodcastScreen(
    state: PodcastState,
    onNavigateBack: () -> Unit,
    onPlayVideo: (String) -> Unit,
    onPlayQueue: (String, List<Video>, Boolean) -> Unit,
    onOpenChannel: (String) -> Unit,
    onAction: (PodcastAction) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.podcast_title)) },
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
                state.isLoading && state.podcast == null -> FullScreenLoader()
                state.errorMessage != null && state.podcast == null -> AnimatedError(
                    message = state.errorMessage,
                    requestId = state.errorRequestId,
                    onRetry = { onAction(PodcastAction.OnRetry) },
                )
                state.podcast != null -> PodcastContent(
                    state = state,
                    onPlayVideo = onPlayVideo,
                    onPlayQueue = onPlayQueue,
                    onOpenChannel = onOpenChannel,
                    onLoadMore = { onAction(PodcastAction.OnLoadMore) },
                )
            }
        }
    }
}

@Composable
private fun PodcastContent(
    state: PodcastState,
    onPlayVideo: (String) -> Unit,
    onPlayQueue: (String, List<Video>, Boolean) -> Unit,
    onOpenChannel: (String) -> Unit,
    onLoadMore: () -> Unit,
) {
    val podcast = requireNotNull(state.podcast)
    val menuScope = rememberVideoMenuScope(onOpenChannel)
    val episodes = state.episodes.filterNot(menuScope::isHidden)
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 300.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }, key = "podcast-header") {
            PodcastHeader(
                podcast = podcast,
                loadedCount = episodes.size,
                hasMore = state.nextPage != null,
                onPlay = { onPlayQueue(podcast.title, episodes, false) },
                onShuffle = { onPlayQueue(podcast.title, episodes, true) },
            )
        }
        items(episodes, key = { it.url }, contentType = { "podcast-episode" }) { episode ->
            VideoCard(
                video = episode,
                onClick = { onPlayVideo(episode.url) },
                onChannelClick = episode.uploaderUrl.takeIf(String::isNotBlank)?.let { url ->
                    { onOpenChannel(url) }
                },
                onMenuAction = { action -> menuScope.onAction(action, episode) },
                menuItemState = menuScope.stateFor(episode),
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
        if (episodes.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = stringResource(R.string.podcast_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                )
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }, key = "podcast-pagination") {
            PodcastPagination(
                cursor = state.nextPage,
                loading = state.isLoadingMore,
                failed = state.loadMoreError,
                onLoadMore = onLoadMore,
            )
        }
    }
}

@Composable
private fun PodcastPagination(
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
