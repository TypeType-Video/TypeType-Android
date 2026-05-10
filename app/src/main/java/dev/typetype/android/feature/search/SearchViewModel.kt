package dev.typetype.android.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
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
) : ViewModel() {

    private val _state = MutableStateFlow(SearchState())
    val state = _state.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadHistory()
        observeSuggestions()
    }

    fun onAction(action: SearchAction) {
        when (action) {
            is SearchAction.OnQueryChange -> _state.update {
                it.copy(query = action.query, hasSearched = false, errorMessage = null)
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
                    suggestions = emptyList(),
                    hasSearched = false,
                    errorMessage = null,
                )
            }
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
                    suggestions = emptyList(),
                )
            }
            searchHistoryRepository.addEntry(query)
            searchRepository.search(query).fold(
                onSuccess = { results ->
                    videoMetaRepository.cacheVideos(results)
                    _state.update {
                        it.copy(isLoading = false, results = results, hasSearched = true)
                    }
                    loadHistory()
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(isLoading = false, errorMessage = error.message, hasSearched = true)
                    }
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
