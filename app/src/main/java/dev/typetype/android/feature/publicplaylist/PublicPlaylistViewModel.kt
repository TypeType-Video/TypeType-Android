package dev.typetype.android.feature.publicplaylist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.R
import dev.typetype.android.core.ui.error.UserErrorMapper
import dev.typetype.android.core.ui.navigation.PublicPlaylistRoute
import dev.typetype.android.domain.library.VideoMetaRepository
import dev.typetype.android.domain.library.cacheVideos
import dev.typetype.android.domain.publicplaylist.PublicPlaylistRepository
import dev.typetype.android.domain.publicplaylist.SavedPublicPlaylist
import dev.typetype.android.domain.publicplaylist.SavedPublicPlaylistRepository
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class PublicPlaylistViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: PublicPlaylistRepository,
    private val savedRepository: SavedPublicPlaylistRepository,
    private val videoMetaRepository: VideoMetaRepository,
    private val errorMapper: UserErrorMapper,
) : ViewModel() {
    private val playlistUrl = savedStateHandle.toRoute<PublicPlaylistRoute>().playlistUrl
    private val _state = MutableStateFlow(PublicPlaylistState())
    val state = _state.asStateFlow()
    private var loadJob: Job? = null

    init {
        loadInitial()
        observeSavedState()
        viewModelScope.launch { savedRepository.refresh() }
    }

    fun onAction(action: PublicPlaylistAction) {
        when (action) {
            PublicPlaylistAction.OnRetry -> loadInitial()
            PublicPlaylistAction.OnLoadMore -> loadMore()
            PublicPlaylistAction.OnToggleSaved -> toggleSaved()
        }
    }

    private fun observeSavedState() {
        viewModelScope.launch {
            savedRepository.observe().collect { items ->
                _state.update {
                    it.copy(savedItemId = items.firstOrNull { item -> item.matches(playlistUrl) }?.id)
                }
            }
        }
        viewModelScope.launch {
            savedRepository.observeCanModify().collect { available ->
                _state.update { it.copy(canSave = available) }
            }
        }
    }

    private fun toggleSaved() {
        val snapshot = _state.value
        if (!snapshot.canSave || snapshot.saveInFlight) return
        viewModelScope.launch {
            _state.update { it.copy(saveInFlight = true, saveErrorMessage = null) }
            val result = if (snapshot.savedItemId != null) {
                savedRepository.remove(snapshot.savedItemId)
            } else {
                savedRepository.save(playlistUrl).map { Unit }
            }
            result.onFailure { failure ->
                _state.update {
                    it.copy(
                        saveErrorMessage = errorMapper.message(
                            failure,
                            R.string.public_playlist_save_failed,
                        ),
                    )
                }
            }
            _state.update { it.copy(saveInFlight = false) }
        }
    }

    private fun loadInitial() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    errorRequestId = null,
                    loadMoreError = false,
                )
            }
            repository.load(playlistUrl).fold(
                onSuccess = { page ->
                    _state.update {
                        it.copy(
                            playlist = page.playlist,
                            videos = page.videos,
                            nextPage = page.nextPage,
                            isLoading = false,
                        )
                    }
                    videoMetaRepository.cacheVideos(page.videos)
                },
                onFailure = { failure ->
                    val details = errorMapper.details(failure, R.string.public_playlist_load_failed)
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = details.message,
                            errorRequestId = details.requestId,
                        )
                    }
                },
            )
        }
    }

    private fun loadMore() {
        val snapshot = _state.value
        val cursor = snapshot.nextPage ?: return
        if (snapshot.isLoading || snapshot.isLoadingMore) return
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoadingMore = true, loadMoreError = false) }
            repository.load(playlistUrl, cursor).fold(
                onSuccess = { page ->
                    _state.update {
                        it.copy(
                            videos = (it.videos + page.videos).distinctBy { video -> video.url },
                            nextPage = page.nextPage,
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
}

private fun SavedPublicPlaylist.matches(url: String): Boolean {
    val target = playlistId(url)
    return this.url == url || publicPlaylistId == target || playlistId(this.url) == target
}

private fun playlistId(url: String): String = PLAYLIST_ID_PATTERN.find(url)?.groupValues?.get(1) ?: url

private val PLAYLIST_ID_PATTERN = Regex("[?&]list=([^&]+)")
