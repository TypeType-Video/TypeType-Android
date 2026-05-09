package dev.typetype.android.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.domain.library.LibraryRepository
import dev.typetype.android.domain.library.Playlist
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: LibraryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(LibraryState(isLoading = true))
    val state = combine(
        _state,
        repository.observeHistory(),
        repository.observePlaylists(),
    ) { base, history, allPlaylists ->
        val favorites = allPlaylists.findByName(FAVORITES_PLAYLIST_NAME)
        val watchLater = allPlaylists.findByName(WATCH_LATER_PLAYLIST_NAME)
        val otherPlaylists = allPlaylists.filterNot { it.isSpecial() }
        base.copy(
            history = history,
            favorites = favorites?.videos.orEmpty(),
            watchLater = watchLater?.videos.orEmpty(),
            playlists = otherPlaylists,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = _state.value,
    )

    init {
        refresh()
    }

    fun onAction(action: LibraryAction) {
        when (action) {
            is LibraryAction.OnTabSelect -> _state.update { it.copy(selectedTab = action.tab) }
            LibraryAction.OnRefresh -> refresh()
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            awaitAll(
                async { repository.refreshHistory() },
                async { repository.refreshPlaylists() },
            )
            _state.update { it.copy(isLoading = false) }
        }
    }

    private fun List<Playlist>.findByName(name: String): Playlist? =
        firstOrNull { it.name.equals(name, ignoreCase = true) }

    private fun Playlist.isSpecial(): Boolean =
        name.equals(FAVORITES_PLAYLIST_NAME, ignoreCase = true) ||
            name.equals(WATCH_LATER_PLAYLIST_NAME, ignoreCase = true)

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val FAVORITES_PLAYLIST_NAME = "Favorites"
        const val WATCH_LATER_PLAYLIST_NAME = "Watch later"
    }
}
