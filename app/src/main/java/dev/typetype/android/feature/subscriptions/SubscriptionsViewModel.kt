package dev.typetype.android.feature.subscriptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.R
import dev.typetype.android.core.error.CodedFailure
import dev.typetype.android.core.ui.error.UserErrorMapper
import dev.typetype.android.data.account.ActiveAccountScope
import dev.typetype.android.data.feed.GENERATION_MISMATCH_CODE
import dev.typetype.android.data.feed.INVALID_CURSOR_CODE
import dev.typetype.android.data.feed.STALE_GENERATION_CODE
import dev.typetype.android.domain.feed.HomeFeedRepository
import dev.typetype.android.domain.feed.SubscriptionsPage
import dev.typetype.android.domain.library.VideoMetaRepository
import dev.typetype.android.domain.library.cacheVideos
import dev.typetype.android.domain.subscriptions.SubscriptionsRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val PAGE_SIZE = 12
private const val SERVER_REFRESH_POLL_MS = 1_000L

@HiltViewModel
class SubscriptionsViewModel @Inject constructor(
    private val feedRepository: HomeFeedRepository,
    private val videoMetaRepository: VideoMetaRepository,
    private val errorMapper: UserErrorMapper,
    private val subscriptionsRepository: SubscriptionsRepository,
    private val activeAccountScope: ActiveAccountScope,
) : ViewModel() {

    private val _state = kotlinx.coroutines.flow.MutableStateFlow(SubscriptionsState(isLoading = true))
    val state = _state.asStateFlow()

    private var requestJob: Job? = null
    private var refreshMonitorJob: Job? = null
    private var nextCursor: String? = null
    private var generation: Long? = null
    private var persistCurrentGeneration = true

    init {
        observeSyncState()
        observeAccountChanges()
        refresh()
    }

    fun onAction(action: SubscriptionsAction) {
        when (action) {
            SubscriptionsAction.OnRefresh -> refresh()
            SubscriptionsAction.OnLoadMore -> loadMore()
            SubscriptionsAction.OnRetrySync -> retrySync()
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
        requestJob?.cancel()
        refreshMonitorJob?.cancel()
        requestJob = viewModelScope.launch { loadFirstPage() }
    }

    private suspend fun loadFirstPage() {
        nextCursor = null
        generation = null
        if (_state.value.videos.isEmpty()) {
            val cached = cachedVideos()
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
        feedRepository.loadSubscriptionsFeed(cursor = null, limit = PAGE_SIZE).fold(
            onSuccess = { page -> acceptFirstPage(page, hadCachedContent) },
            onFailure = ::showRefreshFailure,
        )
    }

    private suspend fun acceptFirstPage(page: SubscriptionsPage, hadCachedContent: Boolean) {
        generation = page.generation
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
                limit = PAGE_SIZE,
                expectedGeneration = expectedGeneration,
            ).fold(
                onSuccess = { page -> acceptContinuation(page) },
                onFailure = { failure ->
                    if (failure.requiresPaginationRestart()) {
                        refreshMonitorJob?.cancel()
                        loadFirstPage()
                    } else {
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
                val result = feedRepository.loadSubscriptionsFeed(cursor = null, limit = PAGE_SIZE)
                val page = result.getOrElse { failure ->
                    showRefreshFailure(failure)
                    return@launch
                }
                if (generation != expectedGeneration) return@launch
                if (page.generation != expectedGeneration) {
                    requestJob?.cancel()
                    expectedGeneration = page.generation
                    generation = page.generation
                    nextCursor = page.nextCursor
                    persistCurrentGeneration = true
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isLoadingMore = true,
                            videos = page.videos.distinctBy { video -> video.url },
                            hasMore = page.hasMore,
                            isServerRefreshing = page.refreshing,
                            generatedAtMillis = page.generatedAtMillis,
                            errorMessage = null,
                            errorRequestId = null,
                            loadMoreError = false,
                        )
                    }
                    persistPage(page, append = false)
                    _state.update { it.copy(isLoadingMore = false) }
                } else {
                    _state.update {
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

    private suspend fun cachedVideos() = try {
        feedRepository.loadCachedSubscriptionsFeed()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Throwable) {
        emptyList()
    }

    private suspend fun persistPage(page: SubscriptionsPage, append: Boolean) {
        try {
            videoMetaRepository.cacheVideos(page.videos)
            feedRepository.cacheSubscriptionsFeed(page.videos, append)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Throwable) {
            showRefreshFailure(failure)
        }
    }

    private fun showRefreshFailure(failure: Throwable) {
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
}

private fun Throwable.requiresPaginationRestart(): Boolean =
    (this as? CodedFailure)?.failureCode in setOf(
        INVALID_CURSOR_CODE,
        STALE_GENERATION_CODE,
        GENERATION_MISMATCH_CODE,
    )
