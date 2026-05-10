package dev.typetype.android.feature.library.playlist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.core.ui.navigation.PlaylistRoute
import dev.typetype.android.domain.library.LibraryRepository
import dev.typetype.android.domain.library.PlaylistVideo
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlaylistDetailState(
    val playlistId: String = "",
    val title: String = "",
    val videos: List<PlaylistVideo> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: LibraryRepository,
) : ViewModel() {

    private val route: PlaylistRoute = savedStateHandle.toRoute()

    private val _state = MutableStateFlow(PlaylistDetailState())
    val state = _state.asStateFlow()

    init {
        _state.update { it.copy(playlistId = route.playlistId) }
        viewModelScope.launch {
            repository.observePlaylists().collect { playlists ->
                val playlist = playlists.firstOrNull { it.id == route.playlistId }
                if (playlist == null) {
                    _state.update { it.copy(isLoading = false) }
                } else {
                    _state.update {
                        it.copy(
                            playlistId = playlist.id,
                            title = playlist.name,
                            videos = playlist.videos,
                            isLoading = false,
                        )
                    }
                }
            }
        }
    }
}
