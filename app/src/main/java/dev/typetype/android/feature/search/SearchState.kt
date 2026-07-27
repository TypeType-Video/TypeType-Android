package dev.typetype.android.feature.search

import dev.typetype.android.domain.feed.Video
import dev.typetype.android.domain.search.SearchChannel
import dev.typetype.android.domain.search.SearchFilterOption
import dev.typetype.android.domain.search.SearchPlaylist

data class SearchState(
    val query: String = "",
    val isLoading: Boolean = false,
    val results: List<Video> = emptyList(),
    val channels: List<SearchChannel> = emptyList(),
    val playlists: List<SearchPlaylist> = emptyList(),
    val errorMessage: String? = null,
    val errorRequestId: String? = null,
    val hasSearched: Boolean = false,
    val searchHistory: List<String> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val contentFilters: List<SearchFilterOption> = emptyList(),
    val sortFilters: List<SearchFilterOption> = emptyList(),
    val selectedContentFilter: String? = null,
    val selectedSortFilter: String? = null,
    val searchSuggestion: String? = null,
    val isCorrectedSearch: Boolean = false,
    val nextPage: String? = null,
    val isLoadingMore: Boolean = false,
    val loadMoreError: Boolean = false,
)
