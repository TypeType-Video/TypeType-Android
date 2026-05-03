package dev.typetype.android.feature.search

import dev.typetype.android.domain.feed.Video

data class SearchState(
    val query: String = "",
    val isLoading: Boolean = false,
    val results: List<Video> = emptyList(),
    val errorMessage: String? = null,
    val hasSearched: Boolean = false,
    val searchHistory: List<String> = emptyList(),
)
