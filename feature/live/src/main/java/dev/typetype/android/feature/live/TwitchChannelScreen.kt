package dev.typetype.android.feature.live

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.typetype.android.R
import dev.typetype.android.core.ui.components.AnimatedError
import dev.typetype.android.core.ui.components.LazyPaginationFooter
import dev.typetype.android.core.ui.components.ProfileAvatar
import dev.typetype.android.core.ui.components.VideoCard
import dev.typetype.android.core.ui.components.VideoCardSkeleton
import dev.typetype.android.domain.twitch.TwitchChannel

@Composable
fun TwitchChannelRouteScreen(viewModel: TwitchChannelViewModel, onPlayVideo: (String) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    TwitchChannelContent(state, onPlayVideo, viewModel::select, viewModel::retry, viewModel::loadMore)
}

@Composable
internal fun TwitchChannelContent(
    state: TwitchChannelState,
    onPlayVideo: (String) -> Unit,
    onSelect: (TwitchChannelTab) -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 280.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // Always present, so the header arriving after the videos does not push the grid out of view.
        item(key = "channel-header", span = { GridItemSpan(maxLineSpan) }) {
            state.channel?.let { ChannelHeader(it) } ?: Spacer(Modifier.height(HEADER_HEIGHT))
        }
        item(key = "channel-tabs", span = { GridItemSpan(maxLineSpan) }) {
            SecondaryTabRow(selectedTabIndex = state.tab.ordinal) {
                Tab(selected = state.tab == TwitchChannelTab.Videos, onClick = { onSelect(TwitchChannelTab.Videos) },
                    text = { Text(stringResource(R.string.twitch_channel_videos)) })
                Tab(selected = state.tab == TwitchChannelTab.Clips, onClick = { onSelect(TwitchChannelTab.Clips) },
                    text = { Text(stringResource(R.string.twitch_channel_clips)) })
            }
        }
        when {
            state.isLoading -> items(SKELETONS, key = { "channel-skeleton-$it" }) { VideoCardSkeleton() }
            state.errorMessage != null -> item(key = "channel-error", span = { GridItemSpan(maxLineSpan) }) {
                AnimatedError(message = state.errorMessage, onRetry = onRetry)
            }
            state.videos.isEmpty() -> item(key = "channel-empty", span = { GridItemSpan(maxLineSpan) }) {
                Text(stringResource(R.string.twitch_channel_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else -> items(state.videos, key = { it.url }, contentType = { "channel-video" }) { video ->
                VideoCard(video = video, onClick = { onPlayVideo(video.url) })
            }
        }
        item(key = "channel-pagination", span = { GridItemSpan(maxLineSpan) }) {
            LazyPaginationFooter(state.cursor, state.isLoadingMore, state.loadMoreError, onLoadMore)
        }
    }
}

@Composable
private fun ChannelHeader(channel: TwitchChannel) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        ProfileAvatar(imageUrl = channel.avatarUrl, fallbackLetter = channel.displayName.take(1), contentDescription = null, modifier = Modifier.size(64.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(channel.displayName, style = MaterialTheme.typography.titleLarge)
            Text(
                pluralStringResource(R.plurals.twitch_channel_followers, channel.followers.toInt().coerceAtLeast(0), formatCount(channel.followers)),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (channel.description.isNotBlank()) {
                Text(channel.description, style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

private fun formatCount(count: Long): String = when {
    count >= 1_000_000 -> "%.1fM".format(count / 1_000_000.0)
    count >= 1_000 -> "%.1fK".format(count / 1_000.0)
    else -> count.toString()
}

private const val SKELETONS = 6
private val HEADER_HEIGHT = 64.dp
