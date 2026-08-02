package dev.typetype.android.domain.channel

import dev.typetype.android.domain.feed.Video
import dev.typetype.android.domain.search.SearchPlaylist

enum class ChannelSort {
    Latest,
    Popular,
    Oldest,
}

data class ChannelQuery(
    val channelUrl: String,
    val sort: ChannelSort = ChannelSort.Latest,
    val searchQuery: String = "",
    val live: Boolean = false,
)

data class Channel(
    val name: String,
    val description: String,
    val avatarUrl: String,
    val bannerUrl: String?,
    val subscriberCount: Long,
    val verified: Boolean,
    val videos: List<Video>,
)

data class ChannelPage(
    val channel: Channel,
    val nextPage: String?,
)

data class ChannelPlaylistsPage(
    val playlists: List<SearchPlaylist>,
    val nextPage: String?,
)
