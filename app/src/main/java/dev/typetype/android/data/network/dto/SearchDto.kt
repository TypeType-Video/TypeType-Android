package dev.typetype.android.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class SearchResponse(
    val items: List<VideoItem> = emptyList(),
    val nextpage: String? = null,
    val searchSuggestion: String? = null,
    val isCorrectedSearch: Boolean = false,
)
