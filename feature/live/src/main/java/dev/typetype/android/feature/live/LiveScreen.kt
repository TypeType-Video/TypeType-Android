package dev.typetype.android.feature.live

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.typetype.android.R
import dev.typetype.android.core.ui.components.AnimatedError
import dev.typetype.android.core.ui.components.LazyPaginationFooter
import dev.typetype.android.core.ui.components.SectionHeader
import dev.typetype.android.core.ui.components.VideoCard
import dev.typetype.android.core.ui.components.VideoCardSkeleton

@Composable
fun LiveRouteScreen(
    viewModel: LiveViewModel,
    onPlayVideo: (String) -> Unit,
    onOpenChannel: (String) -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LiveContent(
        state = state,
        onPlayVideo = onPlayVideo,
        onOpenChannel = onOpenChannel,
        onRetry = viewModel::refresh,
        onLoadMore = viewModel::loadMore,
        onSearch = viewModel::search,
        onSelectCategory = viewModel::selectCategory,
    )
}

@Composable
internal fun LiveContent(
    state: LiveState,
    onPlayVideo: (String) -> Unit,
    onRetry: () -> Unit,
    onOpenChannel: (String) -> Unit = {},
    onLoadMore: () -> Unit,
    onSearch: (String) -> Unit = {},
    onSelectCategory: (String?) -> Unit = {},
) {
    if (state.errorMessage != null && state.videos.isEmpty()) {
        AnimatedError(message = state.errorMessage, requestId = state.errorRequestId, onRetry = onRetry)
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 280.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item(key = "live-search", span = { GridItemSpan(maxLineSpan) }) { LiveSearchField(state.query, onSearch) }
        items(state.channels, key = { "channel-${it.login}" }, span = { GridItemSpan(maxLineSpan) }) { channel ->
            LiveChannelRow(channel, onOpen = { onOpenChannel(channel.url) })
        }
        if (state.followed.isNotEmpty()) {
            item(key = "followed", span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionHeader(text = stringResource(R.string.live_followed))
                    LiveVideoRail(state.followed, onPlayVideo, onOpenChannel)
                }
            }
        }
        if (state.replays.isNotEmpty()) {
            item(key = "replays", span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionHeader(text = stringResource(R.string.live_followed_replays))
                    LiveVideoRail(state.replays, onPlayVideo, onOpenChannel)
                }
            }
        }
        if (state.followedChannels.isNotEmpty()) {
            item(key = "followed-channels", span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionHeader(text = stringResource(R.string.live_followed_channels))
                    LiveChannelRail(state.followedChannels, onOpenChannel)
                }
            }
        }
        item(key = "live-categories", span = { GridItemSpan(maxLineSpan) }) {
            LiveCategoryChips(state.categories, state.category, onSelectCategory)
        }
        item(key = "live-header", span = { GridItemSpan(maxLineSpan) }) {
            SectionHeader(text = stringResource(R.string.live_popular))
        }
        if (state.isLoading && state.videos.isEmpty()) {
            items(SKELETONS, key = { "live-skeleton-$it" }) { VideoCardSkeleton() }
        }
        items(state.videos, key = { it.url }, contentType = { "live-video" }) { video ->
            VideoCard(
                video = video,
                onClick = { onPlayVideo(video.url) },
                onChannelClick = { onOpenChannel(video.uploaderUrl) },
            )
        }
        item(key = "live-pagination", span = { GridItemSpan(maxLineSpan) }) {
            LazyPaginationFooter(
                continuationKey = state.cursor,
                isLoading = state.isLoadingMore,
                hasError = state.loadMoreError,
                onLoadMore = onLoadMore,
            )
        }
    }
}

private const val SKELETONS = 6
