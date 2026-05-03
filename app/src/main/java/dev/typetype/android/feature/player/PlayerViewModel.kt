package dev.typetype.android.feature.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.core.ui.navigation.PlayerRoute
import dev.typetype.android.data.comments.CommentsPagingSource
import dev.typetype.android.domain.comments.Comment
import dev.typetype.android.domain.comments.CommentsRepository
import dev.typetype.android.domain.stream.StreamRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class PlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val streamRepository: StreamRepository,
    commentsRepository: CommentsRepository,
) : ViewModel() {

    private val route: PlayerRoute = savedStateHandle.toRoute<PlayerRoute>()

    private val _state = MutableStateFlow(PlayerState(videoUrl = route.videoUrl))
    val state = _state.asStateFlow()

    val comments: Flow<PagingData<Comment>> = Pager(
        config = PagingConfig(pageSize = 30, prefetchDistance = 10, enablePlaceholders = false),
        pagingSourceFactory = { CommentsPagingSource(commentsRepository, route.videoUrl) },
    ).flow.cachedIn(viewModelScope)

    init {
        loadStream()
    }

    private fun loadStream() {
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            streamRepository.loadStream(route.videoUrl).fold(
                onSuccess = { stream ->
                    _state.update {
                        it.copy(isLoading = false, stream = stream, errorMessage = null)
                    }
                },
                onFailure = { throwable ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: "Could not load stream",
                        )
                    }
                },
            )
        }
    }
}
