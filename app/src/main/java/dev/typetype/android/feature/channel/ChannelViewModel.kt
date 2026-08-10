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
import dev.typetype.android.domain.channel.ChannelQuery
import dev.typetype.android.domain.channel.ChannelSort
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

    private val _state = MutableStateFlow(
        ChannelState(supportsYouTubeDiscovery = channelUrl.isYouTubeChannel()),
    )
    val state = _state.asStateFlow()

    private var loadJob: Job? = null
    private var subscribeJob: Job? = null
    private var podcastsJob: Job? = null
    private var playlistsJob: Job? = null

    init {
        load()
        loadPodcasts()
        observeSubscription()
    }

    fun onAction(action: ChannelAction) {
        when (action) {
            ChannelAction.OnRefresh -> {
                if (_state.value.tab == ChannelTab.Playlists) {
                    loadPlaylists(force = true)
                } else {
                    load()
                    if (_state.value.tab == ChannelTab.Videos) loadPodcasts()
                }
            }
            ChannelAction.OnLoadMore -> loadMore()
            ChannelAction.OnLoadMorePlaylists -> loadMorePlaylists()
            ChannelAction.OnToggleSubscribe -> toggleSubscribe()
            ChannelAction.OnSubmitSearch -> submitSearch()
            ChannelAction.OnDismissSearch -> {
                _state.update { it.copy(searchInput = it.appliedSearch) }
            }
            ChannelAction.OnClearSearchInput -> {
                _state.update { it.copy(searchInput = "") }
            }
            ChannelAction.OnClearSearch -> clearSearch()
            is ChannelAction.OnSearchInputChanged -> {
                _state.update { it.copy(searchInput = action.value) }
            }
            is ChannelAction.OnSelectSort -> selectSort(action.sort)
            is ChannelAction.OnSelectTab -> selectTab(action.tab)
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
                it.copy(
                    isLoading = true,
                    isLoadingMore = false,
                    loadMoreError = false,
                    errorMessage = null,
                    errorRequestId = null,
                )
            }
            channelRepository.loadChannel(currentQuery()).fold(
                onSuccess = { page ->
                    videoMetaRepository.cacheVideos(page.channel.videos)
                    _state.update { it.finishChannelLoad(page) }
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

    private fun loadMore() {
        val snapshot = _state.value
        val cursor = snapshot.nextPage ?: return
        if (snapshot.channel == null) return
        if (snapshot.isLoading || snapshot.isLoadingMore) return
        loadJob?.cancel()
        val query = currentQuery(snapshot)
        loadJob = viewModelScope.launch {
            _state.update(ChannelState::startPageLoad)
            channelRepository.loadChannel(query, cursor).fold(
                onSuccess = { page ->
                    videoMetaRepository.cacheVideos(page.channel.videos)
                    _state.update { it.appendPage(page, cursor) }
                },
                onFailure = { error ->
                    val details = errorMapper.details(error, R.string.channel_load_failed)
                    _state.update { it.failPageLoad(details.message, details.requestId) }
                },
            )
        }
    }

    private fun selectTab(tab: ChannelTab) {
        val current = _state.value
        if (!current.supportsYouTubeDiscovery || current.tab == tab) return
        _state.update {
            it.copy(
                tab = tab,
                searchInput = "",
                appliedSearch = "",
                isLoading = if (tab == ChannelTab.Playlists) false else it.isLoading,
                isLoadingMore = false,
                errorMessage = null,
                errorRequestId = null,
            )
        }
        if (tab == ChannelTab.Playlists) {
            loadJob?.cancel()
            loadPlaylists()
        } else {
            load()
        }
    }

    private fun selectSort(sort: ChannelSort) {
        val current = _state.value
        if (current.tab != ChannelTab.Videos || current.appliedSearch.isNotEmpty()) return
        if (current.sort == sort) return
        _state.update { it.copy(sort = sort) }
        load()
    }

    private fun submitSearch() {
        val current = _state.value
        if (!current.supportsYouTubeDiscovery || current.tab != ChannelTab.Videos) return
        val query = current.searchInput.trim()
        if (query == current.appliedSearch) return
        _state.update { it.copy(searchInput = query, appliedSearch = query) }
        load()
    }

    private fun clearSearch() {
        val current = _state.value
        if (current.searchInput.isEmpty() && current.appliedSearch.isEmpty()) return
        _state.update { it.copy(searchInput = "", appliedSearch = "") }
        if (current.appliedSearch.isNotEmpty()) load()
    }

    private fun loadPlaylists(force: Boolean = false) {
        val current = _state.value
        if (!force && (current.playlistsLoaded || current.playlistsLoading)) return
        playlistsJob?.cancel()
        playlistsJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    playlistsLoading = true,
                    playlistsLoadMoreError = false,
                    playlistsErrorMessage = null,
                    playlistsErrorRequestId = null,
                )
            }
            channelRepository.loadPlaylists(channelUrl).fold(
                onSuccess = { page -> _state.update { it.finishPlaylistsLoad(page) } },
                onFailure = { error ->
                    val details = errorMapper.details(error, R.string.channel_playlists_load_failed)
                    _state.update {
                        it.copy(
                            playlistsLoading = false,
                            playlistsErrorMessage = details.message,
                            playlistsErrorRequestId = details.requestId,
                        )
                    }
                },
            )
        }
    }

    private fun loadMorePlaylists() {
        val current = _state.value
        val cursor = current.playlistsNextPage ?: return
        if (current.playlistsLoading || current.playlistsLoadingMore) return
        playlistsJob?.cancel()
        playlistsJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    playlistsLoadingMore = true,
                    playlistsLoadMoreError = false,
                    playlistsErrorMessage = null,
                    playlistsErrorRequestId = null,
                )
            }
            channelRepository.loadPlaylists(channelUrl, cursor).fold(
                onSuccess = { page -> _state.update { it.appendPlaylistsPage(page, cursor) } },
                onFailure = { error ->
                    val details = errorMapper.details(error, R.string.channel_playlists_load_failed)
                    _state.update {
                        it.copy(
                            playlistsLoadingMore = false,
                            playlistsLoadMoreError = true,
                            playlistsErrorMessage = details.message,
                            playlistsErrorRequestId = details.requestId,
                        )
                    }
                },
            )
        }
    }

    private fun currentQuery(state: ChannelState = _state.value) = ChannelQuery(
        channelUrl = channelUrl,
        sort = state.sort,
        searchQuery = state.appliedSearch,
        live = state.tab == ChannelTab.Live,
    )

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

internal fun String.isYouTubeChannel(): Boolean =
    contains("youtube.com", ignoreCase = true) || startsWith("/channel/") || startsWith("/c/")
