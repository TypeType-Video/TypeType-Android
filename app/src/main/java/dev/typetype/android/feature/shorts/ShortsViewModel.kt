package dev.typetype.android.feature.shorts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.R
import dev.typetype.android.core.ui.error.UserErrorMapper
import dev.typetype.android.domain.feed.HomeFeedRepository
import dev.typetype.android.domain.feed.ShortsContinuation
import dev.typetype.android.domain.feed.Video
import dev.typetype.android.domain.feed.shortIdentity
import dev.typetype.android.domain.library.VideoMetaRepository
import dev.typetype.android.domain.library.cacheVideos
import dev.typetype.android.domain.usersettings.UserSettingsRepository
import dev.typetype.android.domain.usersettings.UserSettings
import dev.typetype.android.feature.player.PlaybackCodecSupport
import javax.inject.Inject
import kotlinx.coroutines.CoroutineStart
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
    private val playbackPreheater: ShortsPlaybackPreheater,
) : ViewModel() {
    private val _state = MutableStateFlow(ShortsState())
    val state = _state.asStateFlow()

    private var continuation: ShortsContinuation? = null
    private var service = 0
    private var loadJob: Job? = null
    private var loadMoreJob: Job? = null
    private val playbackPreloads = mutableMapOf<String, ShortsPlaybackPreload>()

    init {
        viewModelScope.launch {
            userSettingsRepository.observe()
                .map { ShortsConfiguration(it.defaultService, it.hideShorts, it.autoplay) }
                .distinctUntilChanged()
                .collect { configuration ->
                    service = configuration.service
                    if (configuration.hidden) {
                        loadJob?.cancel()
                        loadMoreJob?.cancel()
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

    internal fun preheatPlayback(
        videoUrl: String,
        settings: UserSettings,
        codecSupport: PlaybackCodecSupport,
        prepareSession: Boolean,
    ) {
        val jobToStart = synchronized(playbackPreloads) {
            playbackPreloads[videoUrl]?.takeIf { it.job.isActive }?.let {
                it.prepareSession = it.prepareSession || prepareSession
                return
            }
            val preload = ShortsPlaybackPreload(prepareSession)
            val job = viewModelScope.launch(start = CoroutineStart.LAZY) {
                playbackPreheater.preheat(videoUrl, settings, codecSupport) {
                    synchronized(playbackPreloads) { preload.prepareSession }
                }
            }
            preload.job = job
            playbackPreloads[videoUrl] = preload
            job.invokeOnCompletion {
                synchronized(playbackPreloads) {
                    if (playbackPreloads[videoUrl] === preload) playbackPreloads.remove(videoUrl)
                }
            }
            job
        }
        jobToStart.start()
    }

    private fun refresh() {
        loadJob?.cancel()
        loadMoreJob?.cancel()
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
        val firstContinuation = continuation ?: return
        val current = _state.value
        if (current.isLoading || current.isLoadingMore || loadMoreJob?.isActive == true) return
        _state.update { it.copy(isLoadingMore = true, loadMoreError = false) }
        loadMoreJob = viewModelScope.launch {
            val known = _state.value.videos.mapTo(mutableSetOf()) { it.shortIdentity() }
            val additions = mutableListOf<Video>()
            var requested = firstContinuation
            var next: ShortsContinuation? = firstContinuation
            var failure: Throwable? = null
            var attempts = 0
            while (attempts < MAX_EMPTY_PAGE_SKIPS && additions.isEmpty()) {
                requested = next ?: break
                feedRepository.loadShorts(requested, service, SHORTS_PAGE_SIZE).fold(
                    onSuccess = { page ->
                        val merged = mergeShortsPage(known, requested, page)
                        additions += merged.additions
                        next = merged.continuation
                    },
                    onFailure = { error -> failure = error },
                )
                if (failure != null) break
                attempts++
            }
            if (attempts == MAX_EMPTY_PAGE_SKIPS && additions.isEmpty()) next = null
            failure?.let {
                _state.update { it.copy(isLoadingMore = false, loadMoreError = true) }
                return@launch
            }
            continuation = next
            _state.update {
                it.copy(
                    videos = it.videos + additions,
                    isLoadingMore = false,
                    hasMore = next != null,
                )
            }
            videoMetaRepository.cacheVideos(additions)
        }
    }
}

private data class ShortsConfiguration(
    val service: Int,
    val hidden: Boolean,
    val autoplay: Boolean,
)

private class ShortsPlaybackPreload(
    var prepareSession: Boolean,
) {
    lateinit var job: Job
}

private const val MAX_EMPTY_PAGE_SKIPS = 3
