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
import dev.typetype.android.domain.library.LibraryRepository
import dev.typetype.android.domain.preferences.PreferencesRepository
import dev.typetype.android.domain.stream.Stream
import dev.typetype.android.domain.stream.StreamRepository
import dev.typetype.android.feature.player.components.PlayerGestureConfig
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class PlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val streamRepository: StreamRepository,
    private val libraryRepository: LibraryRepository,
    private val preferencesRepository: PreferencesRepository,
    commentsRepository: CommentsRepository,
) : ViewModel() {

    private val route: PlayerRoute = savedStateHandle.toRoute<PlayerRoute>()

    private val _state = MutableStateFlow(PlayerState(videoUrl = route.videoUrl))
    val state = _state.asStateFlow()

    private val _events = Channel<PlayerEvent>(Channel.BUFFERED)
    val events: Flow<PlayerEvent> = _events.receiveAsFlow()

    val comments: Flow<PagingData<Comment>> = Pager(
        config = PagingConfig(pageSize = 30, prefetchDistance = 10, enablePlaceholders = false),
        pagingSourceFactory = { CommentsPagingSource(commentsRepository, route.videoUrl) },
    ).flow.cachedIn(viewModelScope)

    init {
        loadStream()
        observePreferences()
    }

    private fun observePreferences() {
        viewModelScope.launch {
            preferencesRepository.observe().collect { prefs ->
                _state.update {
                    it.copy(
                        gestureConfig = PlayerGestureConfig(
                            doubleTapSeekEnabled = prefs.playerDoubleTapSeekEnabled,
                            swipeSeekEnabled = prefs.playerSwipeSeekEnabled,
                            swipeBrightnessVolumeEnabled = prefs.playerSwipeBrightnessVolumeEnabled,
                            longPressSpeedEnabled = prefs.playerLongPressSpeedEnabled,
                        ),
                        autoplayEnabled = prefs.playerAutoplayEnabled,
                    )
                }
            }
        }
    }

    fun onAction(action: PlayerAction) {
        when (action) {
            PlayerAction.OnToggleFavorite -> toggleFavorite()
            PlayerAction.OnToggleWatchLater -> toggleWatchLater()
            is PlayerAction.OnSaveProgress -> viewModelScope.launch {
                libraryRepository.saveProgress(route.videoUrl, action.positionMillis)
            }
        }
    }

    private fun loadStream() {
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            streamRepository.loadStream(route.videoUrl).fold(
                onSuccess = { stream ->
                    _state.update { it.copy(isLoading = false, stream = stream) }
                    checkLibraryStatus()
                    postHistory(stream)
                },
                onFailure = { throwable ->
                    _state.update {
                        it.copy(isLoading = false, errorMessage = throwable.message ?: "Could not load stream")
                    }
                },
            )
        }
    }

    private fun checkLibraryStatus() {
        viewModelScope.launch {
            libraryRepository.loadFavorites().onSuccess { favs ->
                _state.update { it.copy(isFavorited = favs.any { f -> f.videoUrl == route.videoUrl }) }
            }
        }
        viewModelScope.launch {
            libraryRepository.loadWatchLater().onSuccess { items ->
                _state.update { it.copy(isInWatchLater = items.any { w -> w.url == route.videoUrl }) }
            }
        }
    }

    private fun postHistory(stream: Stream) {
        viewModelScope.launch {
            libraryRepository.addHistory(
                videoUrl = route.videoUrl,
                title = stream.title,
                thumbnail = stream.thumbnailUrl,
                duration = stream.durationSeconds,
                channelName = stream.uploaderName,
                channelUrl = stream.uploaderUrl,
            )
        }
    }

    private fun toggleFavorite() {
        val favorited = _state.value.isFavorited
        val title = _state.value.stream?.title.orEmpty()
        viewModelScope.launch {
            if (favorited) {
                libraryRepository.removeFavorite(route.videoUrl).fold(
                    onSuccess = {
                        _state.update { it.copy(isFavorited = false) }
                        _events.send(PlayerEvent.FavoriteRemoved)
                    },
                    onFailure = { _events.send(PlayerEvent.ActionFailed(it.message ?: "")) },
                )
            } else {
                libraryRepository.addFavorite(route.videoUrl).fold(
                    onSuccess = {
                        _state.update { it.copy(isFavorited = true) }
                        _events.send(PlayerEvent.FavoriteAdded(title))
                    },
                    onFailure = { _events.send(PlayerEvent.ActionFailed(it.message ?: "")) },
                )
            }
        }
    }

    private fun toggleWatchLater() {
        val inWatchLater = _state.value.isInWatchLater
        val stream = _state.value.stream ?: return
        viewModelScope.launch {
            if (inWatchLater) {
                libraryRepository.removeWatchLater(route.videoUrl).fold(
                    onSuccess = {
                        _state.update { it.copy(isInWatchLater = false) }
                        _events.send(PlayerEvent.WatchLaterRemoved)
                    },
                    onFailure = { _events.send(PlayerEvent.ActionFailed(it.message ?: "")) },
                )
            } else {
                libraryRepository.addWatchLater(
                    url = route.videoUrl,
                    title = stream.title,
                    thumbnail = stream.thumbnailUrl,
                    duration = stream.durationSeconds,
                ).fold(
                    onSuccess = {
                        _state.update { it.copy(isInWatchLater = true) }
                        _events.send(PlayerEvent.WatchLaterAdded(stream.title))
                    },
                    onFailure = { _events.send(PlayerEvent.ActionFailed(it.message ?: "")) },
                )
            }
        }
    }
}
