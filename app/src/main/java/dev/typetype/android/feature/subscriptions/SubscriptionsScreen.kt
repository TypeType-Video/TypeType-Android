package dev.typetype.android.feature.subscriptions

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.typetype.android.core.ui.components.AnimatedError
import dev.typetype.android.core.ui.components.LazyPaginationFooter
import dev.typetype.android.core.ui.components.VideoCard
import dev.typetype.android.feature.menu.rememberVideoMenuScope
import dev.typetype.android.feature.library.LibrarySyncStatusBar
import dev.typetype.android.R

@Composable
fun SubscriptionsRoute(
    onPlayVideo: (videoUrl: String) -> Unit,
    onOpenChannel: (channelUrl: String) -> Unit = {},
    viewModel: SubscriptionsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SubscriptionsScreen(
        state = state,
        onPlayVideo = onPlayVideo,
        onOpenChannel = onOpenChannel,
        onRetry = { viewModel.onAction(SubscriptionsAction.OnRefresh) },
        onRetrySync = { viewModel.onAction(SubscriptionsAction.OnRetrySync) },
        onLoadMore = { viewModel.onAction(SubscriptionsAction.OnLoadMore) },
        onTabSelect = { viewModel.onAction(SubscriptionsAction.OnTabSelect(it)) },
    )
}

@Composable
fun SubscriptionsScreen(
    state: SubscriptionsState,
    onPlayVideo: (videoUrl: String) -> Unit,
    onOpenChannel: (channelUrl: String) -> Unit,
    onRetry: () -> Unit,
    onRetrySync: () -> Unit,
    onLoadMore: () -> Unit,
    onTabSelect: (SubscriptionsTab) -> Unit,
) {
    val menuScope = rememberVideoMenuScope(onOpenChannel = onOpenChannel)
    val visibleVideos = state.videos.filterNot(menuScope::isHidden)
    Column(modifier = Modifier.fillMaxSize()) {
        LibrarySyncStatusBar(
            isRefreshing = false,
            lastSuccessfulSyncAtMillis = null,
            errorMessage = state.syncErrorMessage,
            requestId = state.syncRequestId,
            pendingWriteCount = state.pendingWriteCount,
            failedWriteCount = state.failedWriteCount,
            onRetry = onRetrySync,
        )
        SubscriptionsFeedStatusBar(
            isRefreshing = state.isLoading,
            isServerRefreshing = state.isServerRefreshing,
            hasPendingRefresh = state.hasPendingRefresh,
            errorMessage = state.errorMessage,
            requestId = state.errorRequestId,
            hasContent = state.videos.isNotEmpty(),
            onRetry = onRetry,
        )
        SubscriptionsHeader(
            selectedTab = state.selectedTab,
            channelCount = state.channels.size,
            onTabSelect = onTabSelect,
        )
        PullToRefreshBox(
            isRefreshing = state.isLoading && state.videos.isNotEmpty(),
            onRefresh = onRetry,
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (state.selectedTab == SubscriptionsTab.Channels) {
                    SubscriptionChannelsGrid(
                        channels = state.channels,
                        isLoading = state.isLoading,
                        onOpenChannel = onOpenChannel,
                    )
                } else {
                    SubscriptionsContent(
                        state = state,
                        visibleVideos = visibleVideos,
                        onPlayVideo = onPlayVideo,
                        onOpenChannel = onOpenChannel,
                        onRetry = onRetry,
                        onLoadMore = onLoadMore,
                        menuScope = menuScope,
                    )
                }
            }
        }
    }
}

@Composable
private fun SubscriptionsContent(
    state: SubscriptionsState,
    visibleVideos: List<dev.typetype.android.domain.feed.Video>,
    onPlayVideo: (videoUrl: String) -> Unit,
    onOpenChannel: (channelUrl: String) -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    menuScope: dev.typetype.android.feature.menu.VideoMenuScope,
) {
    when {
        state.isLoading && state.videos.isEmpty() -> SubscriptionsLoadingGrid()
        state.errorMessage != null && state.videos.isEmpty() ->
            AnimatedError(
                message = state.errorMessage,
                requestId = state.errorRequestId,
                onRetry = onRetry,
            )
        visibleVideos.isEmpty() && !state.isLoading -> SubscriptionsEmptyState()
        else -> {
            val gridState = rememberLazyGridState()
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 320.dp),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            ) {
                items(visibleVideos, key = { it.id }, contentType = { "subscription-video" }) { video ->
                    VideoCard(
                        video = video,
                        onClick = { onPlayVideo(video.url) },
                        onChannelClick = { onOpenChannel(video.uploaderUrl) },
                        onMenuAction = { action -> menuScope.onAction(action, video) },
                        menuItemState = menuScope.stateFor(video),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                    )
                }
                item(key = "subscriptions-pagination", span = { GridItemSpan(maxLineSpan) }) {
                    LazyPaginationFooter(
                        continuationKey = state.videos.size.takeIf { state.hasMore },
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
private fun SubscriptionsEmptyState() {
    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = androidx.compose.foundation.shape.CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Subscriptions,
                    contentDescription = null,
                    modifier = Modifier.padding(18.dp).size(36.dp),
                )
            }
            Text(
                text = stringResource(R.string.subscriptions_empty_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(R.string.subscriptions_empty_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}
