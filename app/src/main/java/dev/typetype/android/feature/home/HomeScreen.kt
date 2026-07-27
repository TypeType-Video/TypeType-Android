package dev.typetype.android.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.typetype.android.R
import dev.typetype.android.core.ui.components.AnimatedError
import dev.typetype.android.core.ui.components.FullScreenLoader
import dev.typetype.android.core.ui.components.LazyPaginationFooter
import dev.typetype.android.core.ui.components.SectionHeader
import dev.typetype.android.core.ui.components.VideoCard
import dev.typetype.android.feature.menu.rememberVideoMenuScope

@Composable
fun HomeRoute(
    viewModel: HomeViewModel,
    onPlayVideo: (videoUrl: String) -> Unit,
    onOpenChannel: (channelUrl: String) -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeScreen(
        state = state,
        onPlayVideo = onPlayVideo,
        onOpenChannel = onOpenChannel,
        onRetry = { viewModel.onAction(HomeAction.OnRefresh) },
        onLoadMore = { viewModel.onAction(HomeAction.OnLoadMore) },
    )
}

@Composable
fun HomeScreen(
    state: HomeState,
    onPlayVideo: (videoUrl: String) -> Unit,
    onOpenChannel: (channelUrl: String) -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
) {
    val menuScope = rememberVideoMenuScope(onOpenChannel = onOpenChannel)
    val visibleVideos = state.videos.filterNot(menuScope::isHidden)
    val continueWatching = if (state.hideContinueWatching) emptyList() else state.continueWatching
    val showRecommendations = !state.hideHomeRecommendations
    when {
        state.isLoading && state.videos.isEmpty() && continueWatching.isEmpty() -> FullScreenLoader()
        state.errorMessage != null && state.videos.isEmpty() && continueWatching.isEmpty() -> AnimatedError(
            message = state.errorMessage,
            requestId = state.errorRequestId,
            onRetry = onRetry,
        )
        continueWatching.isEmpty() && (!showRecommendations || visibleVideos.isEmpty()) -> HomeEmptyState()
        else -> LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 280.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            if (continueWatching.isNotEmpty()) {
                item(key = "continue-watching", span = { GridItemSpan(maxLineSpan) }) {
                    ContinueWatchingSection(
                        items = continueWatching,
                        onPlayVideo = onPlayVideo,
                        onOpenChannel = onOpenChannel,
                    )
                }
            }
            if (state.isLoading && showRecommendations) {
                item(key = "home-refresh", span = { GridItemSpan(maxLineSpan) }) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
            if (showRecommendations) {
                item(key = "home-header", span = { GridItemSpan(maxLineSpan) }) {
                    SectionHeader(
                        text = stringResource(
                            when (state.feedKind) {
                                HomeFeedKind.Recommended -> R.string.home_section_recommended
                                HomeFeedKind.Trending -> R.string.home_section_trending
                            },
                        ),
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
                items(
                    visibleVideos,
                    key = { "home-${it.id}" },
                    contentType = { "home-video" },
                ) { video ->
                    VideoCard(
                        video = video,
                        onClick = { onPlayVideo(video.url) },
                        onChannelClick = { onOpenChannel(video.uploaderUrl) },
                        onMenuAction = { action -> menuScope.onAction(action, video) },
                        menuItemState = menuScope.stateFor(video),
                    )
                }
                item(key = "home-pagination", span = { GridItemSpan(maxLineSpan) }) {
                    LazyPaginationFooter(
                        continuationKey = state.nextCursor,
                        isLoading = state.isLoadingMore,
                        hasError = state.loadMoreError,
                        onLoadMore = onLoadMore,
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeEmptyState() {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.home_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
