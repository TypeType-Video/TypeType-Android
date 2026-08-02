package dev.typetype.android.feature.channel

import dev.typetype.android.domain.channel.Channel
import dev.typetype.android.domain.channel.ChannelSort
import dev.typetype.android.domain.podcast.Podcast
import dev.typetype.android.domain.search.SearchPlaylist

enum class ChannelTab {
    Videos,
    Live,
    Playlists,
}

data class ChannelState(
    val isLoading: Boolean = true,
    val channel: Channel? = null,
    val tab: ChannelTab = ChannelTab.Videos,
    val sort: ChannelSort = ChannelSort.Latest,
    val searchInput: String = "",
    val appliedSearch: String = "",
    val supportsYouTubeDiscovery: Boolean = false,
    val nextPage: String? = null,
    val isLoadingMore: Boolean = false,
    val loadMoreError: Boolean = false,
    val errorMessage: String? = null,
    val errorRequestId: String? = null,
    val isSubscribed: Boolean = false,
    val subscribeInFlight: Boolean = false,
    val podcasts: List<Podcast> = emptyList(),
    val podcastsLoading: Boolean = false,
    val playlists: List<SearchPlaylist> = emptyList(),
    val playlistsLoaded: Boolean = false,
    val playlistsNextPage: String? = null,
    val playlistsLoading: Boolean = false,
    val playlistsLoadingMore: Boolean = false,
    val playlistsLoadMoreError: Boolean = false,
    val playlistsErrorMessage: String? = null,
    val playlistsErrorRequestId: String? = null,
)
