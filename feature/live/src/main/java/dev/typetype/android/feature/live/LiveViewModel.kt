package dev.typetype.android.feature.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.R
import dev.typetype.android.core.ui.error.UserErrorMapper
import dev.typetype.android.domain.feed.Video
import dev.typetype.android.domain.twitch.TwitchRepository
import javax.inject.Inject
import kotlinx.coroutines.Job
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
)

@HiltViewModel
class LiveViewModel @Inject constructor(
    private val twitch: TwitchRepository,
    private val errorMapper: UserErrorMapper,
) : ViewModel() {
    private val _state = MutableStateFlow(LiveState())
    val state = _state.asStateFlow()
    private var loadJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        loadJob?.cancel()
        _state.update { it.copy(isLoading = true, errorMessage = null, errorRequestId = null) }
        loadJob = viewModelScope.launch {
            twitch.popular().fold(
                onSuccess = { page -> _state.value = LiveState(videos = page.items, cursor = page.cursor) },
                onFailure = { failure ->
                    val details = errorMapper.details(failure, R.string.live_load_failed)
                    _state.update { it.copy(isLoading = false, errorMessage = details.message, errorRequestId = details.requestId) }
                },
            )
        }
    }

    fun loadMore() {
        val current = _state.value
        val cursor = current.cursor ?: return
        if (current.isLoading || current.isLoadingMore) return
        _state.update { it.copy(isLoadingMore = true, loadMoreError = false) }
        loadJob = viewModelScope.launch {
            twitch.popular(cursor).fold(
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
}
