package dev.typetype.android.feature.subscriptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.R
import dev.typetype.android.core.ui.error.UserErrorMapper
import dev.typetype.android.data.account.ActiveAccountScope
import dev.typetype.android.data.network.NetworkAvailabilityObserver
import dev.typetype.android.domain.feed.HomeFeedRepository
import dev.typetype.android.domain.feed.SubscriptionsPage
import dev.typetype.android.domain.library.VideoMetaRepository
import dev.typetype.android.domain.library.cacheVideos
import dev.typetype.android.domain.subscriptions.SubscriptionsRepository
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@HiltViewModel
class SubscriptionsViewModel @Inject constructor(
    private val feedRepository: HomeFeedRepository,
    private val videoMetaRepository: VideoMetaRepository,
    private val errorMapper: UserErrorMapper,
    private val subscriptionsRepository: SubscriptionsRepository,
    private val channelsProvider: SubscriptionChannelsProvider,
    private val activeAccountScope: ActiveAccountScope,
    private val networkAvailabilityObserver: NetworkAvailabilityObserver,
) : ViewModel() {

    private val _state = kotlinx.coroutines.flow.MutableStateFlow(SubscriptionsState(isLoading = true))
    val state = _state.asStateFlow()

    private var requestJob: Job? = null
    private var refreshMonitorJob: Job? = null
    private var nextCursor: String? = null
    private var generation: Long? = null
    private var persistCurrentGeneration = true
    private val networkRecovery = SubscriptionsRecovery()

    init {
        observeSyncState()
        observeChannels()
        observeAccountChanges()
        observeNetworkRecovery()
        refresh()
    }

    fun onAction(action: SubscriptionsAction) {
        when (action) {
            SubscriptionsAction.OnRefresh -> refresh()
            SubscriptionsAction.OnLoadMore -> loadMore()
            SubscriptionsAction.OnRetrySync -> retrySync()
            is SubscriptionsAction.OnTabSelect -> _state.update { it.copy(selectedTab = action.tab) }
        }
    }

    private fun observeChannels() {
        viewModelScope.launch {
            channelsProvider.channels.collect { channels -> _state.update { it.copy(channels = channels) } }
        }
    }

    private fun observeSyncState() {
        viewModelScope.launch {
            subscriptionsRepository.observeSyncState().collect { sync ->
                val failure = sync?.takeIf { it.failedWriteCount > 0 }?.let {
                    errorMapper.message(
                        failureCode = it.writeFailureCode,
                        statusCode = it.writeFailureStatusCode,
                        fallbackRes = R.string.library_changes_sync_failed,
                    )
                }
                _state.update {
                    it.copy(
                        syncErrorMessage = failure,
                        syncRequestId = sync?.writeRequestId,
                        lastSuccessfulSyncAtMillis = sync?.lastSuccessAtMillis,
                        pendingWriteCount = sync?.pendingWriteCount ?: 0,
                        failedWriteCount = sync?.failedWriteCount ?: 0,
                    )
                }
            }
        }
    }

    private fun observeAccountChanges() {
        viewModelScope.launch {
            activeAccountScope.observe().filterNotNull().drop(1).collect {
                requestJob?.cancel()
                refreshMonitorJob?.cancel()
                nextCursor = null
                generation = null
                persistCurrentGeneration = true
                networkRecovery.clear()
                _state.value = SubscriptionsState(isLoading = true)
                refresh()
            }
        }
    }

    private fun retrySync() {
        viewModelScope.launch {
            subscriptionsRepository.retryPendingWrites().onFailure { failure ->
                val details = errorMapper.details(failure, R.string.library_changes_sync_failed)
                _state.update {
                    it.copy(syncErrorMessage = details.message, syncRequestId = details.requestId)
                }
            }
        }
    }

    private fun refresh() {
        networkRecovery.clear()
        requestJob?.cancel()
        refreshMonitorJob?.cancel()
        requestJob = viewModelScope.launch { loadFirstPage() }
        viewModelScope.launch { channelsProvider.refresh() }
    }

    private suspend fun loadFirstPage() {
        nextCursor = null
        generation = null
        if (_state.value.videos.isEmpty()) {
            val cached = feedRepository.loadCachedSubscriptionsFeedOrEmpty()
            if (cached.isNotEmpty()) _state.update { it.copy(videos = cached) }
        }
        val hadCachedContent = _state.value.videos.isNotEmpty()
        _state.update {
            it.copy(
                isLoading = true,
                isLoadingMore = false,
                isServerRefreshing = false,
                errorMessage = null,
                errorRequestId = null,
                loadMoreError = false,
            )
        }
        feedRepository.loadSubscriptionsFeed(cursor = null, limit = SUBSCRIPTIONS_PAGE_SIZE).fold(
            onSuccess = { page -> acceptFirstPage(page, hadCachedContent) },
            onFailure = ::showRefreshFailure,
        )
    }

    private suspend fun acceptFirstPage(page: SubscriptionsPage, hadCachedContent: Boolean) {
        generation = page.generation
        networkRecovery.clear()
        nextCursor = page.nextCursor
        persistCurrentGeneration = !page.refreshing || !hadCachedContent
        val isPersisting = persistCurrentGeneration
        _state.update {
            it.copy(
                isLoading = false,
                isLoadingMore = isPersisting,
                videos = page.videos.distinctBy { video -> video.url },
                hasMore = page.hasMore,
                isServerRefreshing = page.refreshing,
                generatedAtMillis = page.generatedAtMillis,
                errorMessage = null,
                errorRequestId = null,
                loadMoreError = false,
            )
        }
        if (isPersisting) persistPage(page, append = false)
        _state.update { it.copy(isLoadingMore = false) }
        if (page.refreshing) monitorServerRefresh(page.generation)
    }

    private fun loadMore() {
        val cursor = nextCursor ?: return
        val expectedGeneration = generation ?: return
        val current = _state.value
        if (current.isLoading || current.isLoadingMore || !current.hasMore) return
        networkRecovery.clear()
        requestJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoadingMore = true,
                    loadMoreError = false,
                    errorMessage = null,
                    errorRequestId = null,
                )
            }
            feedRepository.loadSubscriptionsFeed(
                cursor = cursor,
                limit = SUBSCRIPTIONS_PAGE_SIZE,
                expectedGeneration = expectedGeneration,
            ).fold(
                onSuccess = { page -> acceptContinuation(page) },
                onFailure = { failure ->
                    if (failure.requiresSubscriptionsPaginationRestart()) {
                        refreshMonitorJob?.cancel()
                        requestJob = viewModelScope.launch { loadFirstPage() }
                    } else {
                        networkRecovery.schedule(failure, SubscriptionsRecoveryRequest.Pagination)
                        val details = errorMapper.details(failure, R.string.subscriptions_failed)
                        _state.update {
                            it.copy(
                                isLoadingMore = false,
                                loadMoreError = true,
                                errorMessage = details.message,
                                errorRequestId = details.requestId,
                            )
                        }
                    }
                },
            )
        }
    }

    private suspend fun acceptContinuation(page: SubscriptionsPage) {
        networkRecovery.clear()
        nextCursor = page.nextCursor
        val isPersisting = persistCurrentGeneration
        _state.update {
            it.copy(
                isLoadingMore = isPersisting,
                videos = (it.videos + page.videos).distinctBy { video -> video.url },
                hasMore = page.hasMore,
                isServerRefreshing = it.isServerRefreshing || page.refreshing,
                loadMoreError = false,
                errorMessage = null,
                errorRequestId = null,
            )
        }
        if (isPersisting) persistPage(page, append = true)
        _state.update { it.copy(isLoadingMore = false) }
        if (page.refreshing && refreshMonitorJob?.isActive != true) {
            monitorServerRefresh(page.generation)
        }
    }

    private fun monitorServerRefresh(observedGeneration: Long) {
        refreshMonitorJob?.cancel()
        refreshMonitorJob = viewModelScope.launch {
            var expectedGeneration = observedGeneration
            while (isActive) {
                delay(SERVER_REFRESH_POLL_MS)
                val result = feedRepository.loadSubscriptionsFeed(
                    cursor = null,
                    limit = SUBSCRIPTIONS_PAGE_SIZE,
                )
                val page = result.getOrElse { failure ->
                    showRefreshFailure(failure)
                    return@launch
                }
                when (subscriptionsGenerationAction(generation, expectedGeneration, page.generation)) {
                    SubscriptionsGenerationAction.Stop -> return@launch
                    SubscriptionsGenerationAction.Replace -> {
                        viewModelScope.launch { acceptFirstPage(page, hadCachedContent = true) }
                        return@launch
                    }
                    SubscriptionsGenerationAction.Continue -> _state.update {
                        it.copy(
                            isServerRefreshing = page.refreshing,
                            generatedAtMillis = page.generatedAtMillis,
                        )
                    }
                }
                if (!page.refreshing) return@launch
            }
        }
    }

    private suspend fun persistPage(page: SubscriptionsPage, append: Boolean) {
        try {
            videoMetaRepository.cacheVideos(page.videos)
            feedRepository.cacheSubscriptionsFeed(page.videos, append)
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (failure: Throwable) {
            showRefreshFailure(failure, recoverAfterNetworkChange = false)
        }
    }

    private fun showRefreshFailure(
        failure: Throwable,
        recoverAfterNetworkChange: Boolean = true,
    ) {
        if (recoverAfterNetworkChange) {
            networkRecovery.schedule(failure, SubscriptionsRecoveryRequest.Refresh)
        }
        val details = errorMapper.details(failure, R.string.subscriptions_failed)
        _state.update {
            it.copy(
                isLoading = false,
                isLoadingMore = false,
                isServerRefreshing = false,
                errorMessage = details.message,
                errorRequestId = details.requestId,
            )
        }
    }

    private fun observeNetworkRecovery() {
        viewModelScope.launch {
            networkAvailabilityObserver.states.drop(1).collect { network ->
                when (networkRecovery.takeWhenAvailable(network.isAvailable)) {
                    SubscriptionsRecoveryRequest.Refresh -> refresh()
                    SubscriptionsRecoveryRequest.Pagination -> loadMore()
                    null -> Unit
                }
            }
        }
    }
}
