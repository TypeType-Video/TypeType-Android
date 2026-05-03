package dev.typetype.android.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.data.network.AccessTokenStore
import dev.typetype.android.domain.feed.HomeFeedRepository
import dev.typetype.android.domain.server.ServerRepository
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val serverRepository: ServerRepository,
    private val tokenStore: AccessTokenStore,
    private val feedRepository: HomeFeedRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state = _state.asStateFlow()

    private val eventsChannel = Channel<HomeEvent>(Channel.BUFFERED)
    val events = eventsChannel.receiveAsFlow()

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
            HomeAction.OnSignOutClick -> signOut()
        }
    }

    private fun refreshFeed() {
        loadJob?.cancel()
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        loadJob = viewModelScope.launch {
            feedRepository.loadHomeRecommendations().fold(
                onSuccess = { videos ->
                    _state.update { it.copy(isLoading = false, videos = videos, errorMessage = null) }
                },
                onFailure = { throwable ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: "Could not load recommendations",
                        )
                    }
                },
            )
        }
    }

    private fun signOut() {
        viewModelScope.launch {
            tokenStore.setAccessToken(null)
            serverRepository.clearCurrentServer()
            eventsChannel.send(HomeEvent.NavigateToWelcome)
        }
    }
}
