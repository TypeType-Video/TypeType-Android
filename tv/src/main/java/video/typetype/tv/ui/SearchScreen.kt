package video.typetype.tv.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import video.typetype.sdk.core.Channel
import video.typetype.sdk.core.Playlist
import video.typetype.sdk.core.Video
import video.typetype.tv.data.TvAppState

@Composable
public fun SearchScreen(
    state: TvAppState,
    isActive: Boolean,
    restoredFocusKey: String?,
    onResultFocused: (String) -> Unit,
    onOpenVideo: (Video) -> Unit,
    onOpenChannel: (Channel) -> Unit,
    onOpenPlaylist: (Playlist) -> Unit,
    onSearch: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSearchContentFilter: (String?) -> Unit,
    onSearchSortFilter: (String?) -> Unit,
    onToggleSearchFilter: (String, String, Boolean) -> Unit,
    onLoadMore: () -> Unit,
    searchFocusRequester: FocusRequester,
    topNavigationFocusRequester: FocusRequester,
) {
    var query by remember { mutableStateOf(state.searchQuery) }
    val listState = rememberLazyListState()
    LaunchedEffect(listState, state.searchPage?.nextPage, state.isLoadingMoreSearch) {
        snapshotFlow {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && last >= info.totalItemsCount - 2
        }.distinctUntilChanged().collectLatest { nearEnd ->
            if (nearEnd && state.searchPage?.nextPage != null && !state.isLoadingMoreSearch) onLoadMore()
        }
    }
    val videos = state.searchPage?.videos.orEmpty()
    val channels = state.searchPage?.channels.orEmpty()
    val playlists = state.searchPage?.playlists.orEmpty()
    val hasScrolled by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0 }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(top = 82.dp, bottom = 64.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
        item {
            Column(modifier = Modifier.padding(horizontal = 58.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Search", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                SearchBar(
                    value = query,
                    onSubmitQuery = {
                        query = it
                        onSearchQueryChange(it)
                        onSearch(it)
                    },
                    focusRequester = searchFocusRequester,
                    upFocusRequester = topNavigationFocusRequester,
                )
            }
        }
        if (state.searchSuggestions.isNotEmpty()) item {
            SearchSuggestionRow(state.searchSuggestions) { suggestion ->
                query = suggestion
                onSearch(suggestion)
            }
        }
        state.searchFilters?.let { filters ->
            item {
                SearchFilterRows(
                    filters = filters,
                    selectedContent = state.selectedSearchContentFilter,
                    selectedSort = state.selectedSearchSortFilter,
                    selectedGroups = state.selectedSearchFilters,
                    onContent = onSearchContentFilter,
                    onSort = onSearchSortFilter,
                    onGroup = onToggleSearchFilter,
                )
            }
        }
        if (channels.isNotEmpty()) item {
            ChannelResultRow(channels, onOpenChannel, restoredFocusKey, isActive, onResultFocused)
        }
        if (playlists.isNotEmpty()) item {
            PlaylistResultRow(playlists, onOpenPlaylist, restoredFocusKey, isActive, onResultFocused)
        }
        if (videos.isNotEmpty()) item {
            VideoRow(
                "Videos", videos, onOpenVideo,
                restoreFocusKey = restoredFocusKey,
                focusActive = isActive,
                cinematic = false,
                onFocused = onResultFocused,
            )
        }
        val showDiscovery = state.searchQuery.isBlank() && state.trending.isNotEmpty()
        if (showDiscovery) item {
            VideoRow(
                title = "Popular right now",
                videos = state.trending.take(16),
                onOpenVideo = onOpenVideo,
                restoreFocusKey = restoredFocusKey,
                focusActive = isActive,
                cinematic = false,
                revealFocusedDetails = true,
                onFocused = onResultFocused,
            )
        }
        if (videos.isEmpty() && channels.isEmpty() && playlists.isEmpty() && !showDiscovery) {
            item {
                EmptyScreen(
                    if (state.searchQuery.isBlank()) "What do you want to watch?" else "No results",
                    "Search videos, channels and playlists on your TypeType instance.",
                )
            }
        }
        if (state.isLoadingSearch || state.isLoadingMoreSearch) item {
            Text(
                if (state.isLoadingSearch) "Searching" else "Loading more results",
                modifier = Modifier.padding(horizontal = 58.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        }
        if (hasScrolled) TvNavigationScrollMask()
    }
}

@Composable
private fun ChannelResultRow(
    channels: List<Channel>,
    onOpen: (Channel) -> Unit,
    restoreFocusKey: String?,
    focusActive: Boolean,
    onFocused: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        RowTitle("Channels")
        LazyRow(
            contentPadding = PaddingValues(horizontal = 58.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            items(channels, key = { it.url }) { channel ->
                val key = "channel:${channel.url}"
                SearchChannelCard(
                    channel = channel,
                    restoreFocus = focusActive && restoreFocusKey == key,
                    onFocused = { onFocused(key) },
                    onClick = { onFocused(key); onOpen(channel) },
                )
            }
        }
    }
}
@Composable
private fun SearchChannelCard(
    channel: Channel,
    restoreFocus: Boolean,
    onFocused: () -> Unit,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val focusRequester = remember { FocusRequester() }
    val focused by interaction.collectIsFocusedAsState()
    LaunchedEffect(restoreFocus) { if (restoreFocus) focusRequester.requestFocus() }
    Column(
        modifier = Modifier.width(142.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Surface(
            modifier = Modifier.width(112.dp).height(112.dp)
                .focusRequester(focusRequester)
                .onFocusChanged { if (it.isFocused) onFocused() }
                .border(
                    BorderStroke(
                        if (focused) 3.dp else 0.dp,
                        if (focused) MaterialTheme.colorScheme.primary else Color.Transparent,
                    ),
                    CircleShape,
                ),
            onClick = onClick,
            interactionSource = interaction,
            shape = androidx.tv.material3.ClickableSurfaceDefaults.shape(CircleShape),
        ) {
            AsyncImage(
                model = channel.avatarUrl,
                contentDescription = channel.name,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        }
        Text(channel.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
    }
}
@Composable
private fun PlaylistResultRow(
    playlists: List<Playlist>,
    onOpen: (Playlist) -> Unit,
    restoreFocusKey: String?,
    focusActive: Boolean,
    onFocused: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        RowTitle("Playlists")
        LazyRow(
            contentPadding = PaddingValues(horizontal = 58.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            items(playlists, key = { it.url }) { playlist ->
                val key = "playlist:${playlist.url}"
                SearchPlaylistCard(
                    playlist = playlist,
                    restoreFocus = focusActive && restoreFocusKey == key,
                    onFocused = { onFocused(key) },
                    onClick = { onFocused(key); onOpen(playlist) },
                )
            }
        }
    }
}

@Composable
private fun SearchPlaylistCard(
    playlist: Playlist,
    restoreFocus: Boolean,
    onFocused: () -> Unit,
    onClick: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(restoreFocus) { if (restoreFocus) focusRequester.requestFocus() }
    Column(modifier = Modifier.width(184.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth().height(104.dp)
                .focusRequester(focusRequester)
                .onFocusChanged { if (it.isFocused) onFocused() },
            onClick = onClick,
            shape = androidx.tv.material3.ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
        ) {
            Box {
                AsyncImage(
                    model = playlist.thumbnailUrl,
                    contentDescription = playlist.title,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                )
                playlist.streamCount.takeIf { it >= 0L }?.let { count ->
                    Text(
                        "$count ${if (count == 1L) "video" else "videos"}",
                        modifier = Modifier.align(Alignment.BottomEnd).padding(7.dp)
                            .background(Color.Black.copy(alpha = .8f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                    )
                }
            }
        }
        Text(playlist.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun RowTitle(title: String) {
    Text(
        title,
        modifier = Modifier.padding(horizontal = 58.dp),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.SemiBold,
    )
}
