package dev.typetype.android.feature.library.playlist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.R
import dev.typetype.android.core.ui.error.UserErrorMapper
import dev.typetype.android.core.ui.navigation.PlaylistRoute
import dev.typetype.android.domain.library.LibraryRepository
import dev.typetype.android.domain.library.PlaylistVideo
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlaylistDetailState(
    val playlistId: String = "",
    val title: String = "",
    val description: String = "",
    val videos: List<PlaylistVideo> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isReordering: Boolean = false,
    val isMutationInFlight: Boolean = false,
    val errorMessage: String? = null,
    val errorRequestId: String? = null,
)

sealed interface PlaylistDetailEvent {
    data object Deleted : PlaylistDetailEvent
}

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: LibraryRepository,
    private val errorMapper: UserErrorMapper,
) : ViewModel() {

    private val route: PlaylistRoute = savedStateHandle.toRoute()

    private val _state = MutableStateFlow(PlaylistDetailState())
    val state = _state.asStateFlow()
    private val _events = Channel<PlaylistDetailEvent>(Channel.BUFFERED)
    val events: Flow<PlaylistDetailEvent> = _events.receiveAsFlow()
    private var refreshJob: Job? = null

    init {
        _state.update { it.copy(playlistId = route.playlistId) }
        observePlaylist()
        refresh()
    }

    fun retry() {
        val current = _state.value
        if (!current.isMutationInFlight && !current.isReordering) refresh()
    }

    fun renamePlaylist(name: String) {
        val cleaned = name.trim()
        val current = _state.value
        if (
            cleaned.isEmpty() || cleaned == current.title ||
            current.isMutationInFlight || current.isReordering
        ) return
        viewModelScope.launch {
            _state.update {
                it.copy(isMutationInFlight = true, errorMessage = null, errorRequestId = null)
            }
            repository.renamePlaylist(route.playlistId, cleaned).fold(
                onSuccess = {
                    _state.update { it.copy(title = cleaned, isMutationInFlight = false) }
                },
                onFailure = ::showMutationFailure,
            )
        }
    }

    fun deletePlaylist() {
        if (_state.value.let { it.isMutationInFlight || it.isReordering }) return
        viewModelScope.launch {
            _state.update {
                it.copy(isMutationInFlight = true, errorMessage = null, errorRequestId = null)
            }
            repository.deletePlaylist(route.playlistId).fold(
                onSuccess = { _events.send(PlaylistDetailEvent.Deleted) },
                onFailure = ::showMutationFailure,
            )
        }
    }

    fun moveVideo(videoUrl: String, direction: Int) {
        val current = _state.value
        if (current.isReordering || current.isMutationInFlight || direction !in setOf(-1, 1)) return
        val original = current.videos.sortedBy { it.position }
        val currentIndex = original.indexOfFirst { it.url == videoUrl }
        val targetIndex = currentIndex + direction
        if (currentIndex < 0 || targetIndex !in original.indices) return
        val reordered = original.toMutableList().apply {
            val moving = removeAt(currentIndex)
            add(targetIndex, moving)
        }.mapIndexed { position, video -> video.copy(position = position) }
        _state.update {
            it.copy(
                videos = reordered,
                isReordering = true,
                errorMessage = null,
                errorRequestId = null,
            )
        }
        viewModelScope.launch {
            repository.reorderPlaylist(route.playlistId, reordered.map { it.url }).fold(
                onSuccess = {
                    _state.update { it.copy(isReordering = false) }
                },
                onFailure = { failure ->
                    val details = errorMapper.details(failure, R.string.playlist_reorder_failed)
                    _state.update {
                        it.copy(
                            videos = original,
                            isReordering = false,
                            errorMessage = details.message,
                            errorRequestId = details.requestId,
                        )
                    }
                },
            )
        }
    }

    private fun observePlaylist() {
        viewModelScope.launch {
            repository.observePlaylists().collect { playlists ->
                val playlist = playlists.firstOrNull { it.id == route.playlistId }
                if (playlist != null) {
                    _state.update {
                        it.copy(
                            playlistId = playlist.id,
                            title = playlist.name,
                            description = playlist.description,
                            videos = playlist.videos,
                        )
                    }
                }
            }
        }
    }

    private fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = it.videos.isEmpty(),
                    isRefreshing = it.videos.isNotEmpty(),
                    isReordering = false,
                    errorMessage = null,
                    errorRequestId = null,
                )
            }
            repository.refreshPlaylist(route.playlistId).fold(
                onSuccess = {
                    _state.update { it.copy(isLoading = false, isRefreshing = false) }
                },
                onFailure = { failure ->
                    val details = errorMapper.details(failure, R.string.playlist_load_failed)
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = details.message,
                            errorRequestId = details.requestId,
                        )
                    }
                },
            )
        }
    }

    private fun showMutationFailure(failure: Throwable) {
        val details = errorMapper.details(failure, R.string.playlist_manage_failed)
        _state.update {
            it.copy(
                isMutationInFlight = false,
                errorMessage = details.message,
                errorRequestId = details.requestId,
            )
        }
    }
}
