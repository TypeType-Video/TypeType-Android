package dev.typetype.android.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.domain.search.SearchRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SearchState())
    val state = _state.asStateFlow()

    private var searchJob: Job? = null

    fun onAction(action: SearchAction) {
        when (action) {
            is SearchAction.OnQueryChange -> _state.update { it.copy(query = action.query) }
            is SearchAction.OnSearch -> performSearch()
            is SearchAction.OnClearQuery -> _state.update {
                it.copy(query = "", results = emptyList(), hasSearched = false, errorMessage = null)
            }
        }
    }

    private fun performSearch() {
        val query = _state.value.query.trim()
        if (query.isBlank()) return
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            searchRepository.search(query).fold(
                onSuccess = { results ->
                    _state.update {
                        it.copy(isLoading = false, results = results, hasSearched = true)
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(isLoading = false, errorMessage = error.message, hasSearched = true)
                    }
                },
            )
        }
    }
}
