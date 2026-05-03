package dev.typetype.android.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.domain.library.LibraryRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: LibraryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(LibraryState(isLoading = true))
    val state = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    fun onAction(action: LibraryAction) {
        when (action) {
            is LibraryAction.OnTabSelect -> _state.update { it.copy(selectedTab = action.tab) }
            LibraryAction.OnRefresh -> load()
        }
    }

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val historyDeferred = async { repository.loadHistory() }
            val favoritesDeferred = async { repository.loadFavorites() }
            val watchLaterDeferred = async { repository.loadWatchLater() }
            val playlistsDeferred = async { repository.loadPlaylists() }
            val history = historyDeferred.await().getOrElse { emptyList() }
            val favorites = favoritesDeferred.await().getOrElse { emptyList() }
            val watchLater = watchLaterDeferred.await().getOrElse { emptyList() }
            val playlists = playlistsDeferred.await().getOrElse { emptyList() }
            _state.update {
                it.copy(
                    isLoading = false,
                    history = history,
                    favorites = favorites,
                    watchLater = watchLater,
                    playlists = playlists,
                )
            }
        }
    }
}
