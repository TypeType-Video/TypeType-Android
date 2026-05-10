package dev.typetype.android.feature.subscriptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.domain.feed.HomeFeedRepository
import dev.typetype.android.domain.library.VideoMetaRepository
import dev.typetype.android.domain.library.cacheVideos
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val PAGE_SIZE = 30

@HiltViewModel
class SubscriptionsViewModel @Inject constructor(
    private val feedRepository: HomeFeedRepository,
    private val videoMetaRepository: VideoMetaRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SubscriptionsState(isLoading = true))
    val state = _state.asStateFlow()

    private var loadJob: Job? = null
    private var loadMoreJob: Job? = null
    private var nextPage: Int = 0

    init {
        refresh()
    }

    fun onAction(action: SubscriptionsAction) {
        when (action) {
            SubscriptionsAction.OnRefresh -> refresh()
            SubscriptionsAction.OnLoadMore -> loadMore()
        }
    }

    private fun refresh() {
        loadJob?.cancel()
        loadMoreJob?.cancel()
        nextPage = 0
        loadJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = true,
                    isLoadingMore = false,
                    errorMessage = null,
                    videos = emptyList(),
                    hasMore = true,
                )
            }
            feedRepository.loadSubscriptionsFeed(page = nextPage, limit = PAGE_SIZE).fold(
                onSuccess = { page ->
                    videoMetaRepository.cacheVideos(page.videos)
                    nextPage = 1
                    _state.update {
                        it.copy(
                            isLoading = false,
                            videos = page.videos,
                            hasMore = page.hasMore,
                        )
                    }
                },
                onFailure = { error ->
                    _state.update { it.copy(isLoading = false, errorMessage = error.message) }
                },
            )
        }
    }

    private fun loadMore() {
        val current = _state.value
        if (current.isLoading || current.isLoadingMore || !current.hasMore) return
        loadMoreJob?.cancel()
        loadMoreJob = viewModelScope.launch {
            _state.update { it.copy(isLoadingMore = true) }
            feedRepository.loadSubscriptionsFeed(page = nextPage, limit = PAGE_SIZE).fold(
                onSuccess = { page ->
                    videoMetaRepository.cacheVideos(page.videos)
                    nextPage += 1
                    _state.update {
                        it.copy(
                            isLoadingMore = false,
                            videos = it.videos + page.videos,
                            hasMore = page.hasMore,
                        )
                    }
                },
                onFailure = {
                    _state.update { it.copy(isLoadingMore = false) }
                },
            )
        }
    }
}
