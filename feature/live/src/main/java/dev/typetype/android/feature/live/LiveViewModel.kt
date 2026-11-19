package dev.typetype.android.feature.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.R
import dev.typetype.android.core.ui.error.UserErrorMapper
import dev.typetype.android.domain.feed.Video
import dev.typetype.android.domain.twitch.TwitchAccountRepository
import dev.typetype.android.domain.twitch.TwitchCategory
import dev.typetype.android.domain.twitch.TwitchChannel
import dev.typetype.android.domain.twitch.TwitchPage
import dev.typetype.android.domain.twitch.TwitchRepository
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LiveState(
    val videos: List<Video> = emptyList(),
    val cursor: String? = null,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val loadMoreError: Boolean = false,
    val errorMessage: String? = null,
    val errorRequestId: String? = null,
    val categories: List<TwitchCategory> = emptyList(),
    val category: String? = null,
    val query: String = "",
    val channels: List<TwitchChannel> = emptyList(),
    val followed: List<Video> = emptyList(),
    val replays: List<Video> = emptyList(),
    val followedChannels: List<TwitchChannel> = emptyList(),
)

@HiltViewModel
class LiveViewModel @Inject constructor(
    private val twitch: TwitchRepository,
    private val accounts: TwitchAccountRepository,
    private val errorMapper: UserErrorMapper,
) : ViewModel() {
    private val _state = MutableStateFlow(LiveState())
    val state = _state.asStateFlow()
    private var loadJob: Job? = null
    private var searchJob: Job? = null

    init {
        refresh()
        viewModelScope.launch {
            twitch.categories().onSuccess { page -> _state.update { it.copy(categories = page.items) } }
        }
        // The linked account's own rows, as on the web: who is live now, the past broadcasts of the channels it
        // follows (the way to reach a channel that is offline), and the channels themselves.
        viewModelScope.launch {
            if (accounts.status().getOrNull()?.linked != true) return@launch
            launch { accounts.followedLives().onSuccess { page -> _state.update { it.copy(followed = page.items) } } }
            launch { accounts.followedVideos().onSuccess { page -> _state.update { it.copy(replays = page.items) } } }
            launch {
                accounts.followedChannels().onSuccess { page -> _state.update { it.copy(followedChannels = page.items) } }
            }
        }
    }

    fun refresh() {
        loadJob?.cancel()
        _state.update { it.copy(isLoading = true, errorMessage = null, errorRequestId = null) }
        loadJob = viewModelScope.launch {
            lives(_state.value.category, null).fold(
                onSuccess = { page ->
                    _state.update { it.copy(videos = page.items, cursor = page.cursor, isLoading = false, loadMoreError = false) }
                },
                onFailure = { failure ->
                    val details = errorMapper.details(failure, R.string.live_load_failed)
                    _state.update {
                        it.copy(videos = emptyList(), isLoading = false, errorMessage = details.message, errorRequestId = details.requestId)
                    }
                },
            )
        }
    }

    fun selectCategory(slug: String?) {
        if (slug == _state.value.category) return
        _state.update { it.copy(category = slug, videos = emptyList(), cursor = null) }
        refresh()
    }

    fun search(query: String) {
        val text = query.trim()
        searchJob?.cancel()
        _state.update { it.copy(query = query, channels = if (text.isEmpty()) emptyList() else it.channels) }
        if (text.isEmpty()) return
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            twitch.searchChannels(text).onSuccess { page -> _state.update { it.copy(channels = page.items) } }
        }
    }

    fun loadMore() {
        val current = _state.value
        val cursor = current.cursor ?: return
        if (current.isLoading || current.isLoadingMore) return
        _state.update { it.copy(isLoadingMore = true, loadMoreError = false) }
        loadJob = viewModelScope.launch {
            lives(current.category, cursor).fold(
                onSuccess = { page ->
                    _state.update { state ->
                        state.copy(
                            videos = (state.videos + page.items).distinctBy { it.url },
                            cursor = page.cursor,
                            isLoadingMore = false,
                        )
                    }
                },
                onFailure = { _state.update { it.copy(isLoadingMore = false, loadMoreError = true) } },
            )
        }
    }

    private suspend fun lives(category: String?, cursor: String?): Result<TwitchPage<Video>> =
        if (category == null) twitch.popular(cursor) else twitch.categoryLives(category, cursor)
}

private const val SEARCH_DEBOUNCE_MS = 300L
