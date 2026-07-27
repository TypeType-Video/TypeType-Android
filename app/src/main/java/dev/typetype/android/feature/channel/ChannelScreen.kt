package dev.typetype.android.feature.channel

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.typetype.android.R
import dev.typetype.android.core.ui.components.AnimatedError
import dev.typetype.android.core.ui.components.FullScreenLoader
import dev.typetype.android.core.ui.components.RequestIdRow
import dev.typetype.android.core.ui.components.VideoCard
import dev.typetype.android.domain.channel.Channel
import dev.typetype.android.feature.menu.rememberVideoMenuScope

@Composable
fun ChannelRoute(
    onNavigateBack: () -> Unit,
    onPlayVideo: (videoUrl: String) -> Unit,
    onOpenPodcast: (podcastUrl: String) -> Unit = {},
    viewModel: ChannelViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ChannelScreen(
        state = state,
        onNavigateBack = onNavigateBack,
        onPlayVideo = onPlayVideo,
        onOpenPodcast = onOpenPodcast,
        onAction = viewModel::onAction,
    )
}

@Composable
fun ChannelScreen(
    state: ChannelState,
    onNavigateBack: () -> Unit,
    onPlayVideo: (videoUrl: String) -> Unit,
    onOpenPodcast: (podcastUrl: String) -> Unit,
    onAction: (ChannelAction) -> Unit,
) {
    when {
        state.isLoading -> FullScreenLoader()
        state.errorMessage != null && state.channel == null -> {
            Box(modifier = Modifier.fillMaxSize()) {
                FloatingBack(onNavigateBack = onNavigateBack)
                AnimatedError(
                    message = state.errorMessage,
                    requestId = state.errorRequestId,
                    onRetry = { onAction(ChannelAction.OnRefresh) },
                )
            }
        }
        state.channel != null -> ChannelContent(
            channel = state.channel,
            isSubscribed = state.isSubscribed,
            subscribeInFlight = state.subscribeInFlight,
            actionErrorMessage = state.errorMessage,
            actionErrorRequestId = state.errorRequestId,
            onNavigateBack = onNavigateBack,
            onPlayVideo = onPlayVideo,
            podcasts = state.podcasts,
            podcastsLoading = state.podcastsLoading,
            onOpenPodcast = onOpenPodcast,
            onToggleSubscribe = { onAction(ChannelAction.OnToggleSubscribe) },
        )
    }
}

@Composable
private fun ChannelContent(
    channel: Channel,
    isSubscribed: Boolean,
    subscribeInFlight: Boolean,
    actionErrorMessage: String?,
    actionErrorRequestId: String?,
    onNavigateBack: () -> Unit,
    onPlayVideo: (videoUrl: String) -> Unit,
    podcasts: List<dev.typetype.android.domain.podcast.Podcast>,
    podcastsLoading: Boolean,
    onOpenPodcast: (podcastUrl: String) -> Unit,
    onToggleSubscribe: () -> Unit,
) {
    val menuScope = rememberVideoMenuScope(onOpenChannel = {})
    val visibleVideos = channel.videos.filterNot(menuScope::isHidden)

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 320.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            ChannelHeader(
                channel = channel,
                isSubscribed = isSubscribed,
                subscribeInFlight = subscribeInFlight,
                onNavigateBack = onNavigateBack,
                onToggleSubscribe = onToggleSubscribe,
            )
        }
        if (actionErrorMessage != null) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text(
                        text = actionErrorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    actionErrorRequestId?.let { RequestIdRow(requestId = it) }
                }
            }
        }
        if (podcastsLoading || podcasts.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                ChannelPodcastsSection(
                    podcasts = podcasts,
                    isLoading = podcastsLoading,
                    onOpenPodcast = onOpenPodcast,
                )
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                text = stringResource(R.string.channel_videos_section),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            )
        }
        items(
            items = visibleVideos,
            key = { it.id },
            contentType = { "channel-video" },
        ) { video ->
            VideoCard(
                video = video,
                onClick = { onPlayVideo(video.url) },
                onMenuAction = { action -> menuScope.onAction(action, video) },
                menuItemState = menuScope.stateFor(video),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            )
        }
        if (visibleVideos.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.channel_no_videos),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
