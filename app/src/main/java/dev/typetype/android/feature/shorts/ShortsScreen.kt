package dev.typetype.android.feature.shorts

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import dev.typetype.android.R
import dev.typetype.android.core.ui.components.AnimatedError
import dev.typetype.android.core.ui.components.FullScreenLoader
import dev.typetype.android.core.ui.components.RequestIdRow
import dev.typetype.android.core.ui.components.VideoMenuAction
import dev.typetype.android.core.ui.components.VideoMenuItemState
import dev.typetype.android.domain.feed.Video
import dev.typetype.android.domain.feed.shortIdentity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShortsScreen(
    state: ShortsState,
    onNavigateBack: () -> Unit,
    onPlayVideo: (String) -> Unit,
    onOpenChannel: (String) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    embeddedPlaybackEnabled: Boolean = false,
    onActiveVideoChanged: (Video?) -> Unit = {},
    onUpcomingVideosChanged: suspend (List<Video>) -> Unit = {},
    embeddedPlayback: @Composable (Video, onAdvance: () -> Unit) -> Unit = { _, _ -> },
    menuItemState: (Video) -> VideoMenuItemState = { VideoMenuItemState() },
    onMenuAction: (VideoMenuAction, Video) -> Unit = { _, _ -> },
    onShowComments: ((Video) -> Unit)? = null,
    isSubscribed: (Video) -> Boolean = { false },
    subscriptionInFlight: (Video) -> Boolean = { false },
    onToggleSubscription: (Video) -> Unit = {},
) {
    when {
        state.isLoading && state.videos.isEmpty() -> FullScreenLoader()
        state.errorMessage != null && state.videos.isEmpty() -> AnimatedError(
            message = state.errorMessage,
            requestId = state.errorRequestId,
            onRetry = onRefresh,
        )
        state.hidden -> ShortsMessage(R.string.shorts_hidden)
        state.videos.isEmpty() -> ShortsMessage(R.string.shorts_empty)
        else -> {
            val pagerState = rememberPagerState(pageCount = { state.videos.size })
            val scope = rememberCoroutineScope()
            val currentLoadMore by rememberUpdatedState(onLoadMore)
            val currentActiveVideoChanged by rememberUpdatedState(onActiveVideoChanged)
            val currentUpcomingVideosChanged by rememberUpdatedState(onUpcomingVideosChanged)
            LaunchedEffect(pagerState, state.videos.size, state.hasMore) {
                snapshotFlow { pagerState.settledPage }
                    .map { it >= state.videos.lastIndex - 3 }
                    .distinctUntilChanged()
                    .collect { nearEnd -> if (nearEnd && state.hasMore) currentLoadMore() }
            }
            LaunchedEffect(pagerState, state.videos) {
                snapshotFlow {
                    if (pagerState.isScrollInProgress) null
                    else state.videos.getOrNull(pagerState.settledPage)
                }
                    .distinctUntilChanged()
                    .collect(currentActiveVideoChanged)
            }
            LaunchedEffect(pagerState, state.videos) {
                snapshotFlow {
                    state.videos.drop(pagerState.settledPage + 1).take(SHORTS_PREFETCH_COUNT)
                }.distinctUntilChanged().collectLatest(currentUpcomingVideosChanged)
            }
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                VerticalPager(
                    state = pagerState,
                    key = { state.videos[it].shortIdentity() },
                    beyondViewportPageCount = 1,
                    modifier = Modifier.fillMaxSize().testTag(SHORTS_PAGER_TAG),
                ) { page ->
                    val pageOffset = (
                        pagerState.currentPage - page + pagerState.currentPageOffsetFraction
                    ).absoluteValue
                    ShortPage(
                        video = state.videos[page],
                        isActive = embeddedPlaybackEnabled &&
                            !pagerState.isScrollInProgress && page == pagerState.settledPage,
                        embeddedPlaybackEnabled = embeddedPlaybackEnabled,
                        visuals = shortsPageVisuals(pageOffset),
                        onPlayVideo = onPlayVideo,
                        onOpenChannel = onOpenChannel,
                        menuItemState = menuItemState(state.videos[page]),
                        onMenuAction = { onMenuAction(it, state.videos[page]) },
                        onShowComments = onShowComments?.let { callback ->
                            { callback(state.videos[page]) }
                        },
                        isSubscribed = isSubscribed(state.videos[page]),
                        subscriptionInFlight = subscriptionInFlight(state.videos[page]),
                        onToggleSubscription = { onToggleSubscription(state.videos[page]) },
                        embeddedPlayback = {
                            embeddedPlayback(state.videos[page]) {
                                if (page < state.videos.lastIndex) {
                                    scope.launch { pagerState.animateScrollToPage(page + 1) }
                                }
                            }
                        },
                    )
                }
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.align(Alignment.TopStart).safeDrawingPadding().padding(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.player_back),
                        tint = Color.White,
                    )
                }
                if (state.isLoadingMore) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.BottomEnd).padding(18.dp).size(24.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp,
                    )
                }
                if (state.errorMessage != null || state.loadMoreError) {
                    ShortsInlineError(
                        message = state.errorMessage
                            ?: stringResource(R.string.shorts_load_more_failed),
                        requestId = state.errorRequestId,
                        onRetry = if (state.errorMessage != null) onRefresh else onLoadMore,
                        modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ShortsInlineError(
    message: String,
    requestId: String?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.94f),
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
            Text(message, style = MaterialTheme.typography.bodySmall)
            requestId?.let { RequestIdRow(requestId = it) }
            TextButton(onClick = onRetry) {
                Text(stringResource(R.string.state_retry))
            }
        }
    }
}

@Composable
private fun ShortsMessage(messageRes: Int) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(messageRes),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

internal const val SHORTS_PAGER_TAG = "shorts-pager"
private const val SHORTS_PREFETCH_COUNT = 2
