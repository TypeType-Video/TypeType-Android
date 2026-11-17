package dev.typetype.android.feature.live

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.R
import dev.typetype.android.core.ui.error.UserErrorMapper
import dev.typetype.android.core.ui.navigation.TwitchChannelRoute
import dev.typetype.android.domain.feed.Video
import dev.typetype.android.domain.twitch.TwitchChannel
import dev.typetype.android.domain.twitch.TwitchRepository
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class TwitchChannelTab { Videos, Clips }

data class TwitchChannelState(
    val channel: TwitchChannel? = null,
    val tab: TwitchChannelTab = TwitchChannelTab.Videos,
    val videos: List<Video> = emptyList(),
    val cursor: String? = null,
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val loadMoreError: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class TwitchChannelViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val twitch: TwitchRepository,
    private val errorMapper: UserErrorMapper,
) : ViewModel() {
    private val login = savedStateHandle.toRoute<TwitchChannelRoute>().login
    private val _state = MutableStateFlow(TwitchChannelState())
    val state = _state.asStateFlow()
    private var job: Job? = null

    // The header first: its current live leads the first page of past broadcasts.
    init {
        viewModelScope.launch {
            twitch.channel(login).onSuccess { channel -> _state.update { it.copy(channel = channel) } }
            load(TwitchChannelTab.Videos)
        }
    }

    fun select(tab: TwitchChannelTab) {
        if (tab != _state.value.tab) load(tab)
    }

    fun retry() = load(_state.value.tab)

    private fun load(tab: TwitchChannelTab) {
        job?.cancel()
        _state.update { it.copy(tab = tab, videos = emptyList(), cursor = null, isLoading = true, errorMessage = null) }
        job = viewModelScope.launch {
            page(tab, null).fold(
                onSuccess = { page ->
                    _state.update { it.copy(videos = page.items, cursor = page.cursor, isLoading = false) }
                },
                onFailure = { failure ->
                    val message = errorMapper.details(failure, R.string.live_load_failed).message
                    _state.update { it.copy(isLoading = false, errorMessage = message) }
                },
            )
        }
    }

    fun loadMore() {
        val current = _state.value
        val cursor = current.cursor ?: return
        if (current.isLoading || current.isLoadingMore) return
        _state.update { it.copy(isLoadingMore = true, loadMoreError = false) }
        job = viewModelScope.launch {
            page(current.tab, cursor).fold(
                onSuccess = { page ->
                    _state.update { state ->
                        state.copy(videos = (state.videos + page.items).distinctBy { it.url }, cursor = page.cursor, isLoadingMore = false)
                    }
                },
                onFailure = { _state.update { it.copy(isLoadingMore = false, loadMoreError = true) } },
            )
        }
    }

    // While live, Twitch also lists the broadcast it is recording; the live card stands for it.
    private suspend fun page(tab: TwitchChannelTab, cursor: String?) = when (tab) {
        TwitchChannelTab.Clips -> twitch.channelClips(login, cursor)
        TwitchChannelTab.Videos -> twitch.channelVods(login, cursor).map { page ->
            val live = _state.value.channel?.live?.takeIf { cursor == null }
            page.copy(items = listOfNotNull(live) + page.items.filterNot { live != null && it.isLive })
        }
    }
}
