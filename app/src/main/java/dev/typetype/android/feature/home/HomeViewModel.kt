package dev.typetype.android.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.domain.feed.HomeFeedRepository
import dev.typetype.android.domain.server.ServerRepository
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    serverRepository: ServerRepository,
    private val feedRepository: HomeFeedRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            serverRepository.observeCurrentServer().collect { server ->
                _state.update { it.copy(currentServer = server) }
                if (server != null) refreshFeed()
            }
        }
    }

    fun onAction(action: HomeAction) {
        when (action) {
            HomeAction.OnRefresh -> refreshFeed()
        }
    }

    private fun refreshFeed() {
        loadJob?.cancel()
        _state.update {
            it.copy(
                isLoading = true,
                topSectionError = null,
                recommendationsError = null,
            )
        }
        loadJob = viewModelScope.launch {
            val subsDeferred = async { feedRepository.loadSubscriptionsFeed() }
            val recommendationsDeferred = async { feedRepository.loadHomeRecommendations() }

            val subsResult = subsDeferred.await()
            val subsVideos = subsResult.getOrDefault(emptyList())

            val (topKind, topVideos, topError) = if (subsVideos.isNotEmpty()) {
                Triple(TopSectionKind.Subscriptions, subsVideos, null as String?)
            } else {
                val trendingResult = feedRepository.loadTrending()
                Triple(
                    TopSectionKind.Trending,
                    trendingResult.getOrDefault(emptyList()),
                    trendingResult.exceptionOrNull()?.message,
                )
            }

            val recommendationsResult = recommendationsDeferred.await()

            _state.update {
                it.copy(
                    isLoading = false,
                    topSectionKind = topKind,
                    topSectionVideos = topVideos,
                    topSectionError = topError.takeIf { topVideos.isEmpty() },
                    recommendations = recommendationsResult.getOrDefault(emptyList()),
                    recommendationsError = recommendationsResult.exceptionOrNull()?.message,
                )
            }
        }
    }
}
