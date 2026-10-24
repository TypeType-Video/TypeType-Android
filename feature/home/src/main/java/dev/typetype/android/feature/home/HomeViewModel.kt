package dev.typetype.android.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.R
import dev.typetype.android.core.ui.error.UserErrorMapper
import dev.typetype.android.core.ui.error.UserErrorDetails
import dev.typetype.android.domain.feed.HomeFeedRepository
import dev.typetype.android.domain.library.LibraryRepository
import dev.typetype.android.domain.library.VideoMetaRepository
import dev.typetype.android.domain.library.cacheVideos
import dev.typetype.android.domain.server.ServerRepository
import dev.typetype.android.domain.usersettings.UserSettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

private const val HOME_PAGE_SIZE = 12
private const val CONTINUE_WATCHING_LIMIT = 12

@HiltViewModel
class HomeViewModel @Inject constructor(
    serverRepository: ServerRepository,
    private val feedRepository: HomeFeedRepository,
    private val libraryRepository: LibraryRepository,
    private val videoMetaRepository: VideoMetaRepository,
    private val userSettingsRepository: UserSettingsRepository,
    private val errorMapper: UserErrorMapper,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state = _state.asStateFlow()

    private var loadJob: Job? = null
    private var loadMoreJob: Job? = null
    private var supportingContentJob: Job? = null

    init {
        viewModelScope.launch {
            serverRepository.observeCurrentServer().collect { server ->
                _state.update { it.copy(currentServer = server) }
                if (server != null) {
                    refreshFeed()
                    refreshSupportingContent()
                }
            }
        }
        viewModelScope.launch {
            libraryRepository.observeContinueWatching(CONTINUE_WATCHING_LIMIT).collect { items ->
                _state.update { it.copy(continueWatching = items) }
            }
        }
        viewModelScope.launch {
            userSettingsRepository.observe().collect { settings ->
                _state.update {
                    it.copy(
                        hideHomeRecommendations = settings.hideHomeRecommendations,
                        hideContinueWatching = settings.hideContinueWatching,
                    )
                }
            }
        }
    }

    fun onAction(action: HomeAction) {
        when (action) {
            HomeAction.OnRefresh -> {
                refreshFeed()
                refreshSupportingContent()
            }
            HomeAction.OnLoadMore -> loadMore()
        }
    }

    private fun refreshSupportingContent() {
        supportingContentJob?.cancel()
        supportingContentJob = viewModelScope.launch {
            supervisorScope {
                launch { libraryRepository.refreshHistory() }
                launch { userSettingsRepository.refresh() }
            }
        }
    }

    private fun refreshFeed() {
        loadJob?.cancel()
        loadMoreJob?.cancel()
        _state.update {
            it.copy(
                isLoading = true,
                isLoadingMore = false,
                errorMessage = null,
                errorRequestId = null,
                loadMoreError = false,
            )
        }
        loadJob = viewModelScope.launch {
            if (_state.value.videos.isEmpty()) {
                val cached = runCatching { feedRepository.loadCachedHomeFeed() }
                    .getOrDefault(emptyList())
                if (cached.isNotEmpty()) _state.update { it.copy(videos = cached) }
            }
            val recommendations = feedRepository.loadHomeRecommendations(limit = HOME_PAGE_SIZE)
            val page = recommendations.getOrNull()
            if (page != null && (page.videos.isNotEmpty() || page.nextCursor != null)) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        feedKind = HomeFeedKind.Recommended,
                        videos = page.videos.distinctBy { video -> video.url },
                        nextCursor = page.nextCursor,
                    )
                }
                videoMetaRepository.cacheVideos(page.videos)
                runCatching { feedRepository.cacheHomeFeed(page.videos, append = false) }
                return@launch
            }
            loadTrendingFallback(recommendations.exceptionOrNull())
        }
    }

    private suspend fun loadTrendingFallback(recommendationsFailure: Throwable?) {
        feedRepository.loadTrending().fold(
            onSuccess = { videos ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        feedKind = HomeFeedKind.Trending,
                        videos = videos.distinctBy { video -> video.url },
                        nextCursor = null,
                    )
                }
                videoMetaRepository.cacheVideos(videos)
                runCatching { feedRepository.cacheHomeFeed(videos, append = false) }
            },
            onFailure = { trendingFailure ->
                val failure = recommendationsFailure ?: trendingFailure
                val details: UserErrorDetails = errorMapper.details(failure, R.string.home_feed_failed)
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

    private fun loadMore() {
        val current = _state.value
        val cursor = current.nextCursor ?: return
        if (current.isLoading || current.isLoadingMore || current.feedKind != HomeFeedKind.Recommended) return
        loadMoreJob?.cancel()
        loadMoreJob = viewModelScope.launch {
            _state.update { it.copy(isLoadingMore = true, loadMoreError = false) }
            feedRepository.loadHomeRecommendations(cursor = cursor, limit = HOME_PAGE_SIZE).fold(
                onSuccess = { page ->
                    _state.update {
                        it.copy(
                            isLoadingMore = false,
                            videos = (it.videos + page.videos).distinctBy { video -> video.url },
                            nextCursor = page.nextCursor,
                        )
                    }
                    videoMetaRepository.cacheVideos(page.videos)
                    runCatching { feedRepository.cacheHomeFeed(page.videos, append = true) }
                },
                onFailure = {
                    _state.update { it.copy(isLoadingMore = false, loadMoreError = true) }
                },
            )
        }
    }
}
