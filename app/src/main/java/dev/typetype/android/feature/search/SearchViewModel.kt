package dev.typetype.android.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.R
import dev.typetype.android.core.ui.error.UserErrorMapper
import dev.typetype.android.domain.library.VideoMetaRepository
import dev.typetype.android.domain.library.cacheVideos
import dev.typetype.android.domain.search.SearchRepository
import dev.typetype.android.domain.searchhistory.SearchHistoryRepository
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val SUGGESTIONS_DEBOUNCE_MS = 250L

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository,
    private val searchHistoryRepository: SearchHistoryRepository,
    private val videoMetaRepository: VideoMetaRepository,
    private val errorMapper: UserErrorMapper,
) : ViewModel() {

    private val _state = MutableStateFlow(SearchState())
    val state = _state.asStateFlow()

    private var searchJob: Job? = null
    private var filtersJob: Job? = null

    init {
        loadHistory()
        loadFilters()
        observeSuggestions()
    }

    fun onAction(action: SearchAction) {
        when (action) {
            is SearchAction.OnQueryChange -> {
                searchJob?.cancel()
                _state.update {
                    it.copy(
                        query = action.query,
                        isLoading = false,
                        isLoadingMore = false,
                        hasSearched = false,
                        errorMessage = null,
                        errorRequestId = null,
                    )
                }
            }
            is SearchAction.OnSearch -> performSearch(_state.value.query)
            is SearchAction.OnSuggestionClick -> {
                _state.update { it.copy(query = action.query) }
                performSearch(action.query)
            }
            is SearchAction.OnClearQuery -> _state.update {
                it.copy(
                    query = "",
                    results = emptyList(),
                    channels = emptyList(),
                    playlists = emptyList(),
                    suggestions = emptyList(),
                    hasSearched = false,
                    errorMessage = null,
                    errorRequestId = null,
                    searchSuggestion = null,
                    isCorrectedSearch = false,
                    nextPage = null,
                    loadMoreError = false,
                )
            }
            is SearchAction.OnContentFilterSelect -> selectContentFilter(action.value)
            is SearchAction.OnFilterToggle -> toggleFilter(action.groupKey, action.optionValue)
            SearchAction.OnResetFilters -> resetFilters()
            SearchAction.OnLoadMore -> loadMore()
            is SearchAction.OnDeleteHistoryEntry -> deleteHistoryEntry(action.query)
            is SearchAction.OnHistoryEntryClick -> {
                _state.update { it.copy(query = action.query) }
                performSearch(action.query)
            }
        }
    }

    private fun observeSuggestions() {
        viewModelScope.launch {
            _state
                .map { it.query }
                .distinctUntilChanged()
                .debounce(SUGGESTIONS_DEBOUNCE_MS)
                .flatMapLatest { query ->
                    val trimmed = query.trim()
                    if (trimmed.isBlank()) {
                        flowOf(emptyList())
                    } else {
                        flowOf(searchRepository.suggestions(trimmed).getOrDefault(emptyList()))
                    }
                }
                .collect { suggestions ->
                    _state.update { it.copy(suggestions = suggestions) }
                }
        }
    }

    private fun loadHistory() {
        viewModelScope.launch {
            searchHistoryRepository.loadHistory().onSuccess { history ->
                _state.update { it.copy(searchHistory = history) }
            }
        }
    }

    private fun loadFilters(contentFilter: String? = null, searchAfter: Boolean = false) {
        filtersJob?.cancel()
        filtersJob = viewModelScope.launch {
            searchRepository.filters(contentFilter = contentFilter).fold(
                onSuccess = { filters ->
                    val groups = filters.resolvedGroups()
                    _state.update {
                        it.copy(
                            contentFilters = filters.content,
                            filterGroups = groups,
                            selectedFilters = sanitizeSearchFilters(groups, it.selectedFilters),
                        )
                    }
                },
                onFailure = {
                    _state.update { it.copy(filterGroups = emptyList(), selectedFilters = emptyList()) }
                },
            )
            if (searchAfter) performSearch(_state.value.query)
        }
    }

    private fun performSearch(rawQuery: String) {
        val query = rawQuery.trim()
        if (query.isBlank()) return
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    query = query,
                    isLoading = true,
                    errorMessage = null,
                    errorRequestId = null,
                    suggestions = emptyList(),
                    results = emptyList(),
                    channels = emptyList(),
                    playlists = emptyList(),
                    nextPage = null,
                    loadMoreError = false,
                )
            }
            searchHistoryRepository.addEntry(query)
            val current = _state.value
            searchRepository.search(
                query = query,
                contentFilter = current.selectedContentFilter,
                filters = current.selectedFilters,
            ).fold(
                onSuccess = { page ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            results = page.videos,
                            channels = page.channels,
                            playlists = page.playlists,
                            searchSuggestion = page.suggestion,
                            isCorrectedSearch = page.isCorrected,
                            nextPage = page.nextSearchCursor(),
                            hasSearched = true,
                        )
                    }
                    videoMetaRepository.cacheVideos(page.videos)
                    loadHistory()
                },
                onFailure = { error ->
                    val details = errorMapper.details(error, R.string.search_failed)
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = details.message,
                            errorRequestId = details.requestId,
                            hasSearched = true,
                        )
                    }
                },
            )
        }
    }

    private fun selectContentFilter(value: String?) {
        if (_state.value.selectedContentFilter == value) return
        _state.update { it.copy(selectedContentFilter = value, selectedFilters = emptyList()) }
        loadFilters(contentFilter = value, searchAfter = true)
    }

    private fun toggleFilter(groupKey: String, optionValue: String) {
        val current = _state.value
        val selected = toggleSearchFilter(
            groups = current.filterGroups,
            selected = current.selectedFilters,
            groupKey = groupKey,
            optionValue = optionValue,
        )
        if (selected == current.selectedFilters) return
        _state.update { it.copy(selectedFilters = selected) }
        performSearch(_state.value.query)
    }

    private fun resetFilters() {
        if (_state.value.selectedFilters.isEmpty()) return
        _state.update { it.copy(selectedFilters = emptyList()) }
        performSearch(_state.value.query)
    }

    private fun loadMore() {
        val snapshot = _state.value
        val cursor = snapshot.nextPage ?: return
        if (snapshot.isLoading || snapshot.isLoadingMore || !snapshot.hasSearched) return
        searchJob = viewModelScope.launch {
            _state.update { it.copy(isLoadingMore = true, loadMoreError = false) }
            searchRepository.search(
                query = snapshot.query,
                nextPage = cursor,
                contentFilter = snapshot.selectedContentFilter,
                filters = snapshot.selectedFilters,
            ).fold(
                onSuccess = { page ->
                    _state.update {
                        it.copy(
                            results = (it.results + page.videos).distinctBy { video -> video.url },
                            channels = (it.channels + page.channels).distinctBy { channel -> channel.url },
                            playlists = (it.playlists + page.playlists)
                                .distinctBy { playlist -> playlist.url },
                            nextPage = page.nextSearchCursor(cursor),
                            isLoadingMore = false,
                        )
                    }
                    videoMetaRepository.cacheVideos(page.videos)
                },
                onFailure = {
                    _state.update { it.copy(isLoadingMore = false, loadMoreError = true) }
                },
            )
        }
    }

    private fun deleteHistoryEntry(query: String) {
        viewModelScope.launch {
            searchHistoryRepository.removeEntry(query).onSuccess {
                _state.update { it.copy(searchHistory = it.searchHistory - query) }
            }
        }
    }
}
