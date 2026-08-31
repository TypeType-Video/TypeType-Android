package video.typetype.tv.data

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import video.typetype.sdk.core.SearchRequest
import video.typetype.sdk.core.TypeTypeResult

public fun TvViewModel.updateSearchQuery(query: String) {
    mutableState.value = mutableState.value.copy(
        searchQuery = query,
        searchSuggestions = if (query.isBlank()) emptyList() else mutableState.value.searchSuggestions,
    )
    if (query.isBlank()) return
    viewModelScope.launch {
        delay(250)
        val snapshot = mutableState.value
        if (snapshot.searchQuery != query) return@launch
        when (val result = client.catalog.suggestions(query, snapshot.selectedService)) {
            is TypeTypeResult.Success -> if (mutableState.value.searchQuery == query) {
                mutableState.value = mutableState.value.copy(
                    searchSuggestions = result.value.filter(String::isNotBlank).distinct().take(10),
                )
            }
            is TypeTypeResult.Failure -> Unit
        }
    }
}

public fun TvViewModel.loadSearchFilters(contentFilter: String? = mutableState.value.selectedSearchContentFilter) {
    viewModelScope.launch {
        val service = mutableState.value.selectedService
        when (val result = client.catalog.searchFilters(service, contentFilter)) {
            is TypeTypeResult.Success -> if (mutableState.value.selectedService == service) {
                mutableState.value = mutableState.value.copy(searchFilters = result.value)
            }
            is TypeTypeResult.Failure -> Unit
        }
    }
}

public fun TvViewModel.selectSearchContentFilter(value: String?) {
    mutableState.value = mutableState.value.copy(
        selectedSearchContentFilter = value,
        selectedSearchFilters = emptyMap(),
    )
    loadSearchFilters(value)
    repeatCurrentSearch()
}

public fun TvViewModel.selectSearchSortFilter(value: String?) {
    mutableState.value = mutableState.value.copy(selectedSearchSortFilter = value)
    repeatCurrentSearch()
}

public fun TvViewModel.toggleSearchFilter(group: String, value: String, multiSelect: Boolean) {
    val current = mutableState.value.selectedSearchFilters
    val selected = current[group].orEmpty()
    val updated = if (multiSelect) {
        if (value in selected) selected - value else selected + value
    } else if (selected.singleOrNull() == value) {
        emptyList()
    } else {
        listOf(value)
    }
    mutableState.value = mutableState.value.copy(
        selectedSearchFilters = current.toMutableMap().apply {
            if (updated.isEmpty()) remove(group) else put(group, updated)
        },
    )
    repeatCurrentSearch()
}

public fun TvViewModel.search(query: String) {
    val normalized = query.trim()
    mutableState.value = mutableState.value.copy(
        searchQuery = normalized,
        searchSuggestions = emptyList(),
    )
    if (normalized.isBlank()) return
    viewModelScope.launch {
        val request = mutableState.value.searchRequest(normalized)
        mutableState.value = mutableState.value.copy(isLoadingSearch = true, errorMessage = null)
        when (val result = client.catalog.search(request)) {
            is TypeTypeResult.Success -> if (mutableState.value.searchQuery == normalized) {
                mutableState.value = mutableState.value.copy(
                    searchPage = result.value,
                    isLoadingSearch = false,
                    errorMessage = null,
                )
            }
            is TypeTypeResult.Failure -> if (mutableState.value.searchQuery == normalized) {
                mutableState.value = mutableState.value.copy(
                    isLoadingSearch = false,
                    errorMessage = result.error.toUserMessage(),
                )
            }
        }
    }
}

public fun TvViewModel.loadMoreSearch() {
    val current = mutableState.value.searchPage ?: return
    val nextPage = current.nextPage ?: return
    if (mutableState.value.isLoadingMoreSearch || mutableState.value.searchQuery.isBlank()) return
    viewModelScope.launch {
        mutableState.value = mutableState.value.copy(isLoadingMoreSearch = true)
        when (val result = client.catalog.search(mutableState.value.searchRequest(mutableState.value.searchQuery, nextPage))) {
            is TypeTypeResult.Success -> mutableState.value = mutableState.value.copy(
                searchPage = result.value.copy(
                    videos = (current.videos + result.value.videos).distinctBy { it.id.value },
                    channels = (current.channels + result.value.channels).distinctBy { it.url },
                    playlists = (current.playlists + result.value.playlists).distinctBy { it.url },
                ),
                isLoadingMoreSearch = false,
                errorMessage = null,
            )
            is TypeTypeResult.Failure -> mutableState.value = mutableState.value.copy(
                isLoadingMoreSearch = false,
                errorMessage = result.error.toUserMessage(),
            )
        }
    }
}

private fun TvViewModel.repeatCurrentSearch() {
    mutableState.value.searchQuery.takeIf(String::isNotBlank)?.let(::search)
}

private fun TvAppState.searchRequest(query: String, nextPage: String? = null): SearchRequest = SearchRequest(
    query = query,
    service = selectedService,
    nextPage = nextPage,
    contentFilter = selectedSearchContentFilter,
    sortFilter = selectedSearchSortFilter,
    filters = selectedSearchFilters.values.flatten(),
)
