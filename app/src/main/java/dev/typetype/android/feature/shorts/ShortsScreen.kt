package dev.typetype.android.feature.shorts

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.typetype.android.R
import dev.typetype.android.core.ui.branding.rememberVideoBranding
import dev.typetype.android.core.ui.components.AnimatedError
import dev.typetype.android.core.ui.components.FullScreenLoader
import dev.typetype.android.core.ui.components.RequestIdRow
import dev.typetype.android.core.ui.components.VideoMenuAction
import dev.typetype.android.core.ui.components.VideoMenuItemState
import dev.typetype.android.domain.feed.Video
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShortsScreen(
    state: ShortsState,
    onPlayVideo: (String) -> Unit,
    onOpenChannel: (String) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    embeddedPlaybackEnabled: Boolean = false,
    onActiveVideoChanged: (Video?) -> Unit = {},
    onNextVideoChanged: suspend (Video?) -> Unit = {},
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
            val currentNextVideoChanged by rememberUpdatedState(onNextVideoChanged)
            LaunchedEffect(pagerState, state.videos.size, state.hasMore) {
                snapshotFlow { pagerState.settledPage }
                    .map { it >= state.videos.lastIndex - 3 }
                    .distinctUntilChanged()
                    .collect { nearEnd -> if (nearEnd && state.hasMore) currentLoadMore() }
            }
            LaunchedEffect(pagerState, state.videos) {
                snapshotFlow {
                    if (pagerState.isScrollInProgress) {
                        null
                    } else {
                        state.videos.getOrNull(pagerState.settledPage)
                    }
                }.distinctUntilChanged().collect(currentActiveVideoChanged)
            }
            LaunchedEffect(pagerState, state.videos) {
                snapshotFlow {
                    if (pagerState.isScrollInProgress) {
                        null
                    } else {
                        state.videos.getOrNull(pagerState.settledPage + 1)
                    }
                }.distinctUntilChanged().collectLatest(currentNextVideoChanged)
            }
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                VerticalPager(
                    state = pagerState,
                    key = { state.videos[it].id },
                    modifier = Modifier.fillMaxSize().testTag(SHORTS_PAGER_TAG),
                ) { page ->
                    ShortPage(
                        video = state.videos[page],
                        isActive = embeddedPlaybackEnabled &&
                            page == pagerState.settledPage &&
                            !pagerState.isScrollInProgress,
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
                    onClick = onRefresh,
                    modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
                ) {
                    Icon(
                        Icons.Outlined.Refresh,
                        contentDescription = stringResource(R.string.shorts_refresh),
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
private fun ShortPage(
    video: Video,
    isActive: Boolean,
    onPlayVideo: (String) -> Unit,
    onOpenChannel: (String) -> Unit,
    menuItemState: VideoMenuItemState,
    onMenuAction: (VideoMenuAction) -> Unit,
    onShowComments: (() -> Unit)?,
    isSubscribed: Boolean,
    subscriptionInFlight: Boolean,
    onToggleSubscription: () -> Unit,
    embeddedPlayback: @Composable () -> Unit,
) {
    val branding = rememberVideoBranding(
        sourceUrl = video.url,
        title = video.title,
        thumbnailUrl = video.thumbnailUrl,
        durationSeconds = video.durationSeconds,
    )
    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            model = branding.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (isActive) embeddedPlayback()
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.18f),
                    0.55f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.88f),
                ),
            ),
        )
        if (isActive) {
            ShortsActionRail(
                video = video,
                state = menuItemState,
                onOpenPlayer = { onPlayVideo(video.url) },
                onAction = onMenuAction,
                onShowComments = onShowComments,
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp),
            )
        } else {
            FilledIconButton(
                onClick = { onPlayVideo(video.url) },
                modifier = Modifier.align(Alignment.Center).size(68.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = stringResource(R.string.shorts_play, branding.title),
                    modifier = Modifier.size(38.dp),
                )
            }
        }
        ShortsInfoOverlay(
            video = video,
            title = branding.title,
            isSubscribed = isSubscribed,
            subscriptionInFlight = subscriptionInFlight,
            onOpenChannel = { onOpenChannel(video.uploaderUrl) },
            onToggleSubscription = onToggleSubscription,
            modifier = Modifier.align(Alignment.BottomStart),
        )
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
