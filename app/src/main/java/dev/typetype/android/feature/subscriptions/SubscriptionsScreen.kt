package dev.typetype.android.feature.subscriptions

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.typetype.android.core.ui.components.AnimatedError
import dev.typetype.android.core.ui.components.AnimatedLoader
import dev.typetype.android.core.ui.components.FullScreenLoader
import dev.typetype.android.core.ui.components.VideoCard
import dev.typetype.android.feature.menu.rememberVideoMenuScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

private const val LOAD_MORE_THRESHOLD = 6

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
        onLoadMore = { viewModel.onAction(SubscriptionsAction.OnLoadMore) },
    )
}

@Composable
fun SubscriptionsScreen(
    state: SubscriptionsState,
    onPlayVideo: (videoUrl: String) -> Unit,
    onOpenChannel: (channelUrl: String) -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
) {
    val menuScope = rememberVideoMenuScope(onOpenChannel = onOpenChannel)
    val visibleVideos = state.videos.filterNot(menuScope::isHidden)
    when {
        state.isLoading && state.videos.isEmpty() -> FullScreenLoader()
        state.errorMessage != null && state.videos.isEmpty() ->
            AnimatedError(message = state.errorMessage, onRetry = onRetry)
        visibleVideos.isEmpty() && !state.isLoading -> Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "No videos from your subscriptions yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        else -> {
            val listState = rememberLazyListState()
            val shouldLoadMore by remember(state.videos.size, state.hasMore, state.isLoadingMore) {
                derivedStateOf {
                    if (!state.hasMore || state.isLoadingMore || state.videos.isEmpty()) {
                        false
                    } else {
                        val lastVisible = listState.layoutInfo.visibleItemsInfo
                            .lastOrNull()?.index ?: -1
                        lastVisible >= state.videos.size - LOAD_MORE_THRESHOLD
                    }
                }
            }
            LaunchedEffect(listState, state.hasMore, state.isLoadingMore) {
                snapshotFlow { shouldLoadMore }
                    .distinctUntilChanged()
                    .filter { it }
                    .collect { onLoadMore() }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 4.dp, bottom = 12.dp),
            ) {
                items(visibleVideos, key = { it.id }) { video ->
                    VideoCard(
                        video = video,
                        onClick = { onPlayVideo(video.url) },
                        onChannelClick = { onOpenChannel(video.uploaderUrl) },
                        onMenuAction = { action -> menuScope.onAction(action, video) },
                        menuItemState = menuScope.stateFor(video),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                if (state.isLoadingMore) {
                    item(key = "load-more") {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            AnimatedLoader(size = 56.dp)
                        }
                    }
                }
            }
        }
    }
}
