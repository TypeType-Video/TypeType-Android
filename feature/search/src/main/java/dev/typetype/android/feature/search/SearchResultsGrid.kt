package dev.typetype.android.feature.search

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.typetype.android.R
import dev.typetype.android.core.ui.components.LazyPaginationFooter
import dev.typetype.android.core.ui.components.VideoCard
import dev.typetype.android.feature.menu.VideoMenuScope
import dev.typetype.android.feature.menu.rememberVideoMenuScope

@Composable
fun SearchResultsGrid(
    state: SearchState,
    onPlayVideo: (String) -> Unit,
    onOpenChannel: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onSearchSuggestion: (String) -> Unit,
    onLoadMore: () -> Unit,
) {
    SearchResultsContent(
        state = state,
        onPlayVideo = onPlayVideo,
        onOpenChannel = onOpenChannel,
        onOpenPlaylist = onOpenPlaylist,
        onSearchSuggestion = onSearchSuggestion,
        onLoadMore = onLoadMore,
        menuScope = rememberVideoMenuScope(onOpenChannel),
    )
}

@Composable
internal fun SearchResultsContent(
    state: SearchState,
    onPlayVideo: (String) -> Unit,
    onOpenChannel: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onSearchSuggestion: (String) -> Unit,
    onLoadMore: () -> Unit,
    menuScope: VideoMenuScope,
) {
    val videos = state.results.filterNot(menuScope::isHidden)
    val channels = state.channels.filterNot { it.url in menuScope.blockedChannelUrls }
    val hasResults = videos.isNotEmpty() || channels.isNotEmpty() || state.playlists.isNotEmpty()
    if (!hasResults) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.search_no_results, state.query),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 300.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        state.searchSuggestion?.takeIf(String::isNotBlank)?.let { suggestion ->
            item(span = { GridItemSpan(maxLineSpan) }, key = "search-suggestion") {
                SearchSuggestionBanner(
                    suggestion = suggestion,
                    isCorrected = state.isCorrectedSearch,
                    onClick = { onSearchSuggestion(suggestion) },
                )
            }
        }
        items(channels, key = { "channel-${it.url}" }, contentType = { "channel" }) { channel ->
            SearchChannelCard(channel = channel, onClick = { onOpenChannel(channel.url) })
        }
        items(
            state.playlists,
            key = { "playlist-${it.url}" },
            contentType = { "playlist" },
        ) { playlist ->
            SearchPlaylistCard(playlist = playlist, onClick = { onOpenPlaylist(playlist.url) })
        }
        items(videos, key = { "video-${it.url}" }, contentType = { "video" }) { video ->
            VideoCard(
                video = video,
                onClick = { onPlayVideo(video.url) },
                onChannelClick = { onOpenChannel(video.uploaderUrl) },
                onMenuAction = { action -> menuScope.onAction(action, video) },
                menuItemState = menuScope.stateFor(video),
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }, key = "search-pagination") {
            SearchPaginationFooter(
                cursor = state.nextPage,
                isLoading = state.isLoadingMore,
                hasError = state.loadMoreError,
                onLoadMore = onLoadMore,
            )
        }
    }
}

@Composable
private fun SearchSuggestionBanner(
    suggestion: String,
    isCorrected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isCorrected) Modifier else Modifier.clickable(onClick = onClick)),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(
            text = stringResource(
                if (isCorrected) R.string.search_showing_results_for else R.string.search_did_you_mean,
                suggestion,
            ),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun SearchPaginationFooter(
    cursor: String?,
    isLoading: Boolean,
    hasError: Boolean,
    onLoadMore: () -> Unit,
) {
    LazyPaginationFooter(
        continuationKey = cursor,
        isLoading = isLoading,
        hasError = hasError,
        onLoadMore = onLoadMore,
    )
}
