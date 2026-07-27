package dev.typetype.android.feature.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.R
import dev.typetype.android.core.ui.error.UserErrorMapper
import dev.typetype.android.domain.library.VideoMetaRepository
import dev.typetype.android.domain.library.cacheVideos
import dev.typetype.android.domain.notifications.NotificationsRepository
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val repository: NotificationsRepository,
    private val videoMetaRepository: VideoMetaRepository,
    private val errorMapper: UserErrorMapper,
) : ViewModel() {
    private val _state = MutableStateFlow(NotificationsState())
    val state = _state.asStateFlow()
    private var loadJob: Job? = null

    init {
        loadInitial()
    }

    fun onAction(action: NotificationsAction) {
        when (action) {
            NotificationsAction.Retry -> loadInitial()
            NotificationsAction.LoadMore -> loadMore()
            NotificationsAction.MarkAllRead -> markAllRead()
        }
    }

    private fun loadInitial() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    errorRequestId = null,
                    loadMoreError = false,
                )
            }
            repository.page().fold(
                onSuccess = { page ->
                    _state.update {
                        it.copy(
                            items = page.items,
                            unreadCount = page.unreadCount,
                            nextPage = page.nextPage,
                            isLoading = false,
                        )
                    }
                    videoMetaRepository.cacheVideos(page.items.map { it.video })
                },
                onFailure = { failure ->
                    val details = errorMapper.details(failure, R.string.notifications_load_failed)
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
        val pageNumber = snapshot.nextPage ?: return
        if (snapshot.isLoading || snapshot.isLoadingMore) return
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoadingMore = true, loadMoreError = false) }
            repository.page(pageNumber).fold(
                onSuccess = { page ->
                    _state.update {
                        it.copy(
                            items = (it.items + page.items).distinctBy { item ->
                                "${item.type}:${item.video.id}:${item.createdAtMillis}"
                            },
                            unreadCount = page.unreadCount,
                            nextPage = page.nextPage,
                            isLoadingMore = false,
                        )
                    }
                    videoMetaRepository.cacheVideos(page.items.map { it.video })
                },
                onFailure = {
                    _state.update { it.copy(isLoadingMore = false, loadMoreError = true) }
                },
            )
        }
    }

    private fun markAllRead() {
        if (_state.value.isMarkingRead || _state.value.unreadCount == 0) return
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isMarkingRead = true,
                    actionErrorMessage = null,
                    actionErrorRequestId = null,
                )
            }
            repository.markAllRead().fold(
                onSuccess = {
                    _state.update { it.copy(unreadCount = 0, isMarkingRead = false) }
                },
                onFailure = { failure ->
                    val details = errorMapper.details(failure, R.string.notifications_mark_read_failed)
                    _state.update {
                        it.copy(
                            isMarkingRead = false,
                            actionErrorMessage = details.message,
                            actionErrorRequestId = details.requestId,
                        )
                    }
                },
            )
        }
    }
}
