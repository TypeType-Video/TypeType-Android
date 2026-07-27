package dev.typetype.android.feature.channel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.R
import dev.typetype.android.core.ui.error.UserErrorMapper
import dev.typetype.android.core.ui.navigation.ChannelRoute
import dev.typetype.android.domain.channel.ChannelRepository
import dev.typetype.android.domain.library.VideoMetaRepository
import dev.typetype.android.domain.library.cacheVideos
import dev.typetype.android.domain.podcast.PodcastRepository
import dev.typetype.android.domain.subscriptions.SubscriptionsRepository
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ChannelViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val channelRepository: ChannelRepository,
    private val videoMetaRepository: VideoMetaRepository,
    private val subscriptionsRepository: SubscriptionsRepository,
    private val podcastRepository: PodcastRepository,
    private val errorMapper: UserErrorMapper,
) : ViewModel() {

    private val channelUrl = savedStateHandle.toRoute<ChannelRoute>().channelUrl

    private val _state = MutableStateFlow(ChannelState())
    val state = _state.asStateFlow()

    private var loadJob: Job? = null
    private var subscribeJob: Job? = null
    private var podcastsJob: Job? = null

    init {
        load()
        loadPodcasts()
        observeSubscription()
    }

    fun onAction(action: ChannelAction) {
        when (action) {
            ChannelAction.OnRefresh -> {
                load()
                loadPodcasts()
            }
            ChannelAction.OnToggleSubscribe -> toggleSubscribe()
        }
    }

    private fun loadPodcasts() {
        if (!channelUrl.isYouTubeChannel()) return
        podcastsJob?.cancel()
        podcastsJob = viewModelScope.launch {
            _state.update { it.copy(podcastsLoading = true) }
            podcastRepository.channelPodcasts(channelUrl).fold(
                onSuccess = { page ->
                    videoMetaRepository.cacheVideos(page.episodes)
                    _state.update {
                        it.copy(podcasts = page.podcasts, podcastsLoading = false)
                    }
                },
                onFailure = {
                    _state.update { it.copy(podcasts = emptyList(), podcastsLoading = false) }
                },
            )
        }
    }

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update {
                it.copy(isLoading = true, errorMessage = null, errorRequestId = null)
            }
            channelRepository.loadChannel(channelUrl).fold(
                onSuccess = { channel ->
                    videoMetaRepository.cacheVideos(channel.videos)
                    _state.update { it.copy(isLoading = false, channel = channel) }
                },
                onFailure = { error ->
                    val details = errorMapper.details(error, R.string.channel_load_failed)
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

    private fun observeSubscription() {
        viewModelScope.launch {
            subscriptionsRepository.observeSubscribedChannelUrls().collect { urls ->
                _state.update { it.copy(isSubscribed = channelUrl in urls) }
            }
        }
    }

    private fun toggleSubscribe() {
        val current = _state.value
        if (current.subscribeInFlight) return
        val channel = current.channel ?: return
        subscribeJob?.cancel()
        subscribeJob = viewModelScope.launch {
            _state.update {
                it.copy(subscribeInFlight = true, errorMessage = null, errorRequestId = null)
            }
            val result = if (current.isSubscribed) {
                subscriptionsRepository.unsubscribe(channelUrl)
            } else {
                subscriptionsRepository.subscribe(
                    channelUrl = channelUrl,
                    name = channel.name,
                    avatarUrl = channel.avatarUrl,
                )
            }
            result.onFailure { e ->
                val details = errorMapper.details(e, R.string.channel_action_failed)
                _state.update {
                    it.copy(errorMessage = details.message, errorRequestId = details.requestId)
                }
            }
            _state.update { it.copy(subscribeInFlight = false) }
        }
    }
}

private fun String.isYouTubeChannel(): Boolean =
    contains("youtube.com", ignoreCase = true) || startsWith("/channel/") || startsWith("/c/")
