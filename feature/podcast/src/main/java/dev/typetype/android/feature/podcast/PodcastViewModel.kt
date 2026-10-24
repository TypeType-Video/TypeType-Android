package dev.typetype.android.feature.podcast

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.R
import dev.typetype.android.core.ui.error.UserErrorMapper
import dev.typetype.android.core.ui.navigation.PodcastRoute
import dev.typetype.android.domain.library.VideoMetaRepository
import dev.typetype.android.domain.library.cacheVideos
import dev.typetype.android.domain.podcast.PodcastRepository
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class PodcastViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: PodcastRepository,
    private val videoMetaRepository: VideoMetaRepository,
    private val errorMapper: UserErrorMapper,
) : ViewModel() {
    private val podcastUrl = savedStateHandle.toRoute<PodcastRoute>().podcastUrl
    private val _state = MutableStateFlow(PodcastState())
    val state = _state.asStateFlow()
    private var loadJob: Job? = null

    init {
        loadInitial()
    }

    fun onAction(action: PodcastAction) {
        when (action) {
            PodcastAction.OnRetry -> loadInitial()
            PodcastAction.OnLoadMore -> loadMore()
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
            repository.episodes(podcastUrl).fold(
                onSuccess = { page ->
                    _state.update {
                        it.copy(
                            podcast = page.podcast,
                            episodes = page.episodes.distinctBy { episode -> episode.url },
                            nextPage = page.nextPage,
                            isLoading = false,
                        )
                    }
                    videoMetaRepository.cacheVideos(page.episodes)
                },
                onFailure = { failure ->
                    val details = errorMapper.details(failure, R.string.podcast_load_failed)
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
            repository.episodes(podcastUrl, cursor).fold(
                onSuccess = { page ->
                    _state.update {
                        it.copy(
                            podcast = page.podcast,
                            episodes = (it.episodes + page.episodes)
                                .distinctBy { episode -> episode.url },
                            nextPage = page.nextPage,
                            isLoadingMore = false,
                        )
                    }
                    videoMetaRepository.cacheVideos(page.episodes)
                },
                onFailure = {
                    _state.update { it.copy(isLoadingMore = false, loadMoreError = true) }
                },
            )
        }
    }
}
