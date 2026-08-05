package dev.typetype.android.feature.shorts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.R
import dev.typetype.android.core.ui.error.UserErrorMapper
import dev.typetype.android.domain.feed.HomeFeedRepository
import dev.typetype.android.domain.feed.ShortsContinuation
import dev.typetype.android.domain.library.VideoMetaRepository
import dev.typetype.android.domain.library.cacheVideos
import dev.typetype.android.domain.usersettings.UserSettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val SHORTS_PAGE_SIZE = 30

@HiltViewModel
class ShortsViewModel @Inject constructor(
    private val feedRepository: HomeFeedRepository,
    private val videoMetaRepository: VideoMetaRepository,
    private val userSettingsRepository: UserSettingsRepository,
    private val errorMapper: UserErrorMapper,
) : ViewModel() {
    private val _state = MutableStateFlow(ShortsState())
    val state = _state.asStateFlow()

    private var continuation: ShortsContinuation? = null
    private var service = 0
    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            userSettingsRepository.observe()
                .map { ShortsConfiguration(it.defaultService, it.hideShorts, it.autoplay) }
                .distinctUntilChanged()
                .collect { configuration ->
                    service = configuration.service
                    if (configuration.hidden) {
                        loadJob?.cancel()
                        continuation = null
                        _state.value = ShortsState(
                            isLoading = false,
                            hidden = true,
                            autoplayEnabled = configuration.autoplay,
                        )
                    } else {
                        _state.update { it.copy(autoplayEnabled = configuration.autoplay) }
                        refresh()
                    }
                }
        }
        viewModelScope.launch { userSettingsRepository.refresh() }
    }

    fun onAction(action: ShortsAction) {
        when (action) {
            ShortsAction.Refresh -> refresh()
            ShortsAction.LoadMore -> loadMore()
        }
    }

    private fun refresh() {
        loadJob?.cancel()
        continuation = null
        _state.update {
            it.copy(
                isLoading = true,
                isLoadingMore = false,
                hidden = false,
                errorMessage = null,
                errorRequestId = null,
                loadMoreError = false,
            )
        }
        loadJob = viewModelScope.launch {
            feedRepository.loadShorts(service = service, limit = SHORTS_PAGE_SIZE).fold(
                onSuccess = { page ->
                    continuation = page.continuation
                    _state.update {
                        it.copy(
                            videos = page.videos,
                            isLoading = false,
                            hasMore = page.continuation != null,
                        )
                    }
                    videoMetaRepository.cacheVideos(page.videos)
                },
                onFailure = { failure ->
                    val details = errorMapper.details(failure, R.string.shorts_load_failed)
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
        val next = continuation ?: return
        if (_state.value.isLoading || _state.value.isLoadingMore) return
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoadingMore = true, loadMoreError = false) }
            feedRepository.loadShorts(next, service, SHORTS_PAGE_SIZE).fold(
                onSuccess = { page ->
                    continuation = page.continuation
                    _state.update {
                        it.copy(
                            videos = (it.videos + page.videos).distinctBy { video -> video.id },
                            isLoadingMore = false,
                            hasMore = page.continuation != null,
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

private data class ShortsConfiguration(
    val service: Int,
    val hidden: Boolean,
    val autoplay: Boolean,
)
