package dev.typetype.android.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.domain.search.SearchRepository
import dev.typetype.android.domain.searchhistory.SearchHistoryRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository,
    private val searchHistoryRepository: SearchHistoryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SearchState())
    val state = _state.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadHistory()
    }

    fun onAction(action: SearchAction) {
        when (action) {
            is SearchAction.OnQueryChange -> _state.update { it.copy(query = action.query) }
            is SearchAction.OnSearch -> performSearch()
            is SearchAction.OnClearQuery -> _state.update {
                it.copy(query = "", results = emptyList(), hasSearched = false, errorMessage = null)
            }
            is SearchAction.OnDeleteHistoryEntry -> deleteHistoryEntry(action.query)
            is SearchAction.OnHistoryEntryClick -> {
                _state.update { it.copy(query = action.query) }
                performSearch()
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

    private fun performSearch() {
        val query = _state.value.query.trim()
        if (query.isBlank()) return
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            searchHistoryRepository.addEntry(query)
            searchRepository.search(query).fold(
                onSuccess = { results ->
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
