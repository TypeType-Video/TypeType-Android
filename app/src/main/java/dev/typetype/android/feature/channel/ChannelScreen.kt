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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
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
import dev.typetype.android.core.ui.components.LazyPaginationFooter
import dev.typetype.android.core.ui.components.RequestIdRow
import dev.typetype.android.core.ui.components.VideoCard
import dev.typetype.android.feature.menu.rememberVideoMenuScope
import dev.typetype.android.feature.search.SearchPlaylistCard

@Composable
fun ChannelRoute(
    onNavigateBack: () -> Unit,
    onPlayVideo: (videoUrl: String) -> Unit,
    onOpenPodcast: (podcastUrl: String) -> Unit = {},
    onOpenPlaylist: (playlistUrl: String) -> Unit = {},
    viewModel: ChannelViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ChannelScreen(
        state = state,
        onNavigateBack = onNavigateBack,
        onPlayVideo = onPlayVideo,
        onOpenPodcast = onOpenPodcast,
        onOpenPlaylist = onOpenPlaylist,
        onAction = viewModel::onAction,
    )
}

@Composable
fun ChannelScreen(
    state: ChannelState,
    onNavigateBack: () -> Unit,
    onPlayVideo: (videoUrl: String) -> Unit,
    onOpenPodcast: (podcastUrl: String) -> Unit,
    onOpenPlaylist: (playlistUrl: String) -> Unit,
    onAction: (ChannelAction) -> Unit,
) {
    when {
        state.isLoading && state.channel == null -> FullScreenLoader()
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
            state = state,
            onNavigateBack = onNavigateBack,
            onPlayVideo = onPlayVideo,
            onOpenPodcast = onOpenPodcast,
            onOpenPlaylist = onOpenPlaylist,
            onAction = onAction,
        )
    }
}

@Composable
private fun ChannelContent(
    state: ChannelState,
    onNavigateBack: () -> Unit,
    onPlayVideo: (videoUrl: String) -> Unit,
    onOpenPodcast: (podcastUrl: String) -> Unit,
    onOpenPlaylist: (playlistUrl: String) -> Unit,
    onAction: (ChannelAction) -> Unit,
) {
    val channel = requireNotNull(state.channel)
    val menuScope = rememberVideoMenuScope(onOpenChannel = {})
    val visibleVideos = channel.videos.filterNot(menuScope::isHidden)

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 320.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }, key = "channel-header") {
            ChannelHeader(
                channel = channel,
                isSubscribed = state.isSubscribed,
                subscribeInFlight = state.subscribeInFlight,
                onNavigateBack = onNavigateBack,
                onToggleSubscribe = { onAction(ChannelAction.OnToggleSubscribe) },
            )
        }
        if (state.errorMessage != null && !state.loadMoreError) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "channel-error") {
                ChannelInlineError(state.errorMessage, state.errorRequestId)
            }
        }
        if (state.tab == ChannelTab.Videos && (state.podcastsLoading || state.podcasts.isNotEmpty())) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "channel-podcasts") {
                ChannelPodcastsSection(
                    podcasts = state.podcasts,
                    isLoading = state.podcastsLoading,
                    onOpenPodcast = onOpenPodcast,
                )
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }, key = "channel-controls") {
            ChannelDiscoveryControls(state = state, onAction = onAction)
        }
        if (state.isLoading) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "channel-refreshing") {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
        if (state.tab == ChannelTab.Playlists) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                ChannelSectionTitle(R.string.channel_playlists_section)
            }
            if (state.playlistsLoading && state.playlists.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) { ChannelLoadingRow() }
            } else if (state.playlistsErrorMessage != null && state.playlists.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    AnimatedError(
                        message = state.playlistsErrorMessage,
                        requestId = state.playlistsErrorRequestId,
                        onRetry = { onAction(ChannelAction.OnRefresh) },
                    )
                }
            }
            items(
                items = state.playlists,
                key = { "channel-playlist-${it.url}" },
                contentType = { "channel-playlist" },
            ) { playlist ->
                SearchPlaylistCard(
                    playlist = playlist,
                    onClick = { onOpenPlaylist(playlist.url) },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                )
            }
            if (state.playlistsLoaded && state.playlists.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    ChannelEmptyRow(R.string.channel_no_playlists)
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }, key = "channel-playlists-pagination") {
                LazyPaginationFooter(
                    continuationKey = state.playlistsNextPage,
                    isLoading = state.playlistsLoadingMore,
                    hasError = state.playlistsLoadMoreError,
                    onLoadMore = { onAction(ChannelAction.OnLoadMorePlaylists) },
                )
            }
        } else {
            item(span = { GridItemSpan(maxLineSpan) }) {
                ChannelSectionTitle(
                    if (state.tab == ChannelTab.Live) R.string.channel_live_section
                    else R.string.channel_videos_section,
                )
            }
            items(
                items = visibleVideos,
                key = { it.url },
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
            if (visibleVideos.isEmpty() && !state.isLoading) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    ChannelEmptyRow(
                        if (state.tab == ChannelTab.Live) R.string.channel_no_live
                        else R.string.channel_no_videos,
                    )
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }, key = "channel-pagination") {
                LazyPaginationFooter(
                    continuationKey = state.nextPage,
                    isLoading = state.isLoadingMore,
                    hasError = state.loadMoreError,
                    onLoadMore = { onAction(ChannelAction.OnLoadMore) },
                )
            }
        }
    }
}

@Composable
private fun ChannelInlineError(message: String, requestId: String?) {
    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        requestId?.let { RequestIdRow(requestId = it) }
    }
}

@Composable
private fun ChannelSectionTitle(resource: Int) {
    Text(
        text = stringResource(resource),
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
    )
}

@Composable
private fun ChannelLoadingRow() {
    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ChannelEmptyRow(resource: Int) {
    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(resource),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
