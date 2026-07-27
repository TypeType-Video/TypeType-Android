package dev.typetype.android.domain.publicplaylist

import dev.typetype.android.domain.feed.Video
import dev.typetype.android.domain.search.SearchPlaylist

data class PublicPlaylistPage(
    val playlist: SearchPlaylist,
    val videos: List<Video>,
    val nextPage: String?,
)
