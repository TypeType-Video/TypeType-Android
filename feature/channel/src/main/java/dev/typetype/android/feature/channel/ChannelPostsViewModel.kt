package dev.typetype.android.feature.channel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.core.ui.navigation.ChannelRoute
import dev.typetype.android.domain.channel.ChannelPostsRepository
import dev.typetype.android.domain.channel.CommunityPost
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChannelPostsState(
    val posts: List<CommunityPost> = emptyList(),
    val nextPage: String? = null,
    val loaded: Boolean = false,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val failed: Boolean = false,
    val loadMoreFailed: Boolean = false,
)

@HiltViewModel
class ChannelPostsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ChannelPostsRepository,
) : ViewModel() {
    private val channelUrl = savedStateHandle.toRoute<ChannelRoute>().channelUrl
    private val _state = MutableStateFlow(ChannelPostsState())
    val state: StateFlow<ChannelPostsState> = _state.asStateFlow()
    private var job: Job? = null

    fun ensureLoaded() {
        val current = _state.value
        if (current.loaded || current.isLoading) return
        _state.update { it.copy(isLoading = true, failed = false) }
        job = viewModelScope.launch {
            repository.loadPosts(channelUrl, nextPage = null).fold(
                onSuccess = { page ->
                    _state.update {
                        it.copy(
                            posts = page.posts.distinctBy(CommunityPost::id),
                            nextPage = page.nextPage,
                            loaded = true,
                            isLoading = false,
                        )
                    }
                },
                onFailure = { _state.update { it.copy(isLoading = false, failed = true) } },
            )
        }
    }

    fun loadMore() {
        val current = _state.value
        val nextPage = current.nextPage ?: return
        if (current.isLoading || current.isLoadingMore) return
        _state.update { it.copy(isLoadingMore = true, loadMoreFailed = false) }
        job = viewModelScope.launch {
            repository.loadPosts(channelUrl, nextPage).fold(
                onSuccess = { page ->
                    _state.update { latest ->
                        val known = latest.posts.mapTo(HashSet(), CommunityPost::id)
                        latest.copy(
                            posts = latest.posts + page.posts.filter { known.add(it.id) },
                            nextPage = page.nextPage,
                            isLoadingMore = false,
                        )
                    }
                },
                onFailure = { _state.update { it.copy(isLoadingMore = false, loadMoreFailed = true) } },
            )
        }
    }
}
