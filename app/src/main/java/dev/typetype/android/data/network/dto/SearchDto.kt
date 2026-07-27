package dev.typetype.android.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class SearchResponse(
    val items: List<VideoItem> = emptyList(),
    val channels: List<SearchChannelDto> = emptyList(),
    val playlists: List<SearchPlaylistDto> = emptyList(),
    val nextpage: String? = null,
    val searchSuggestion: String? = null,
    val isCorrectedSearch: Boolean = false,
)

@Serializable
data class SearchChannelDto(
    val id: String,
    val name: String,
    val url: String,
    val thumbnailUrl: String = "",
    val description: String = "",
    val subscriberCount: Long = 0,
    val streamCount: Long = 0,
    val isVerified: Boolean = false,
)

@Serializable
data class SearchPlaylistDto(
    val id: String,
    val title: String,
    val url: String,
    val thumbnailUrl: String = "",
    val uploaderName: String = "",
    val streamCount: Long = 0,
    val playlistType: String = "",
)

@Serializable
data class SearchFiltersResponse(
    val contentFilters: List<SearchFilterOptionDto> = emptyList(),
    val sortFilters: List<SearchFilterOptionDto> = emptyList(),
)

@Serializable
data class SearchFilterOptionDto(
    val value: String,
    val label: String,
)

@Serializable
data class PublicPlaylistResponseDto(
    val playlist: SearchPlaylistDto,
    val videos: List<VideoItem> = emptyList(),
    val nextpage: String? = null,
)
