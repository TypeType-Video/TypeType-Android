package dev.typetype.android.domain.search

import dev.typetype.android.domain.feed.Video

data class SearchPage(
    val videos: List<Video>,
    val channels: List<SearchChannel>,
    val playlists: List<SearchPlaylist>,
    val nextPage: String?,
    val suggestion: String?,
    val isCorrected: Boolean,
)

data class SearchChannel(
    val id: String,
    val name: String,
    val url: String,
    val thumbnailUrl: String,
    val description: String,
    val subscriberCount: Long,
    val streamCount: Long,
    val isVerified: Boolean,
)

data class SearchPlaylist(
    val id: String,
    val title: String,
    val url: String,
    val thumbnailUrl: String,
    val uploaderName: String,
    val streamCount: Long,
    val playlistType: String,
)

data class SearchFilters(
    val content: List<SearchFilterOption>,
    val sort: List<SearchFilterOption>,
    val groups: List<SearchFilterGroup> = emptyList(),
)

data class SearchFilterOption(
    val value: String,
    val label: String,
    val isDefault: Boolean = false,
)

data class SearchFilterGroup(
    val key: String,
    val label: String,
    val multiSelect: Boolean,
    val options: List<SearchFilterOption>,
)
