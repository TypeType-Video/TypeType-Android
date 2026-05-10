package dev.typetype.android.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.data.comments.CommentsPagingSource
import dev.typetype.android.domain.comments.Comment
import dev.typetype.android.domain.comments.CommentsRepository
import dev.typetype.android.domain.library.LibraryRepository
import dev.typetype.android.domain.library.VideoMeta
import dev.typetype.android.domain.library.VideoMetaRepository
import dev.typetype.android.domain.library.cacheVideos
import dev.typetype.android.domain.preferences.PreferencesRepository
import dev.typetype.android.domain.stream.Stream
import dev.typetype.android.domain.stream.StreamRepository
import dev.typetype.android.feature.player.components.PlayerGestureConfig
import dev.typetype.android.feature.player.host.PlayerHostController
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val streamRepository: StreamRepository,
    private val libraryRepository: LibraryRepository,
    private val videoMetaRepository: VideoMetaRepository,
    private val preferencesRepository: PreferencesRepository,
    private val playerHostController: PlayerHostController,
    val commentsRepository: CommentsRepository,
) : ViewModel() {

    private val videoUrlFlow = playerHostController.state
        .map { it.videoUrl }
        .distinctUntilChanged()

    private val _state = MutableStateFlow(PlayerState())
    val state = _state.asStateFlow()

    private val _events = Channel<PlayerEvent>(Channel.BUFFERED)
    val events: Flow<PlayerEvent> = _events.receiveAsFlow()

    val comments: Flow<PagingData<Comment>> = videoUrlFlow
        .flatMapLatest { url ->
            if (url.isNullOrBlank()) flowOf(PagingData.empty())
            else Pager(
                config = PagingConfig(pageSize = 30, prefetchDistance = 10, enablePlaceholders = false),
                pagingSourceFactory = { CommentsPagingSource(commentsRepository, url) },
            ).flow
        }.cachedIn(viewModelScope)

    private var loadStreamJob: Job? = null
    private var favoriteJob: Job? = null
    private var watchLaterJob: Job? = null

    init {
        viewModelScope.launch {
            videoUrlFlow.collect { url ->
                _state.update {
                    it.copy(
                        videoUrl = url.orEmpty(),
                        stream = null,
                        isLoading = !url.isNullOrBlank(),
                        errorMessage = null,
                        isFavorited = false,
                        isInWatchLater = false,
                    )
                }
                if (url.isNullOrBlank()) {
                    loadStreamJob?.cancel()
                    favoriteJob?.cancel()
                    watchLaterJob?.cancel()
                } else {
                    loadStream(url)
                    observeLibraryStatus(url)
                }
            }
        }
        observePreferences()
        viewModelScope.launch {
            libraryRepository.observePlaylists().collect { playlists ->
                _state.update { it.copy(playlists = playlists) }
            }
        }
    }

    private fun observeLibraryStatus(url: String) {
        favoriteJob?.cancel()
        favoriteJob = viewModelScope.launch {
            libraryRepository.observeIsFavorite(url)
                .distinctUntilChanged()
                .collect { isFavorite ->
                    if (currentUrl() == url) {
                        _state.update { it.copy(isFavorited = isFavorite) }
                    }
                }
        }
        watchLaterJob?.cancel()
        watchLaterJob = viewModelScope.launch {
            libraryRepository.observeIsInWatchLater(url)
                .distinctUntilChanged()
                .collect { isInWatchLater ->
                    if (currentUrl() == url) {
                        _state.update { it.copy(isInWatchLater = isInWatchLater) }
                    }
                }
        }
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
            PlayerAction.OnRetry -> currentUrl()?.let { loadStream(it) }
            PlayerAction.OnOpenPlaylistPicker ->
                _state.update { it.copy(playlistPickerVisible = true) }
            PlayerAction.OnDismissPlaylistPicker ->
                _state.update { it.copy(playlistPickerVisible = false) }
            is PlayerAction.OnAddToPlaylist -> addCurrentToPlaylist(action.playlistId)
            is PlayerAction.OnCreatePlaylistAndAdd -> createPlaylistAndAdd(action.name)
            is PlayerAction.OnSaveProgress -> {
                val url = currentUrl() ?: return
                viewModelScope.launch {
                    libraryRepository.saveProgress(url, action.positionMillis)
                }
            }
        }
    }

    private fun addCurrentToPlaylist(playlistId: String) {
        val url = currentUrl() ?: return
        val stream = _state.value.stream ?: return
        val playlistName = _state.value.playlists.firstOrNull { it.id == playlistId }?.name.orEmpty()
        viewModelScope.launch {
            _state.update { it.copy(playlistActionInFlight = true) }
            libraryRepository.addVideoToPlaylist(
                playlistId = playlistId,
                videoUrl = url,
                title = stream.title,
                thumbnail = stream.thumbnailUrl,
                duration = stream.durationSeconds,
                channelName = stream.uploaderName,
                channelUrl = stream.uploaderUrl,
                channelAvatarUrl = stream.uploaderAvatarUrl,
                viewCount = stream.viewCount,
            ).fold(
                onSuccess = {
                    _state.update {
                        it.copy(playlistActionInFlight = false, playlistPickerVisible = false)
                    }
                    _events.send(PlayerEvent.AddedToPlaylist(playlistName))
                },
                onFailure = {
                    _state.update { it.copy(playlistActionInFlight = false) }
                    _events.send(PlayerEvent.ActionFailed(it.message ?: ""))
                },
            )
        }
    }

    private fun createPlaylistAndAdd(name: String) {
        val cleanedName = name.trim()
        if (cleanedName.isEmpty()) return
        val url = currentUrl() ?: return
        val stream = _state.value.stream ?: return
        viewModelScope.launch {
            _state.update { it.copy(playlistActionInFlight = true) }
            libraryRepository.createPlaylist(cleanedName).fold(
                onSuccess = { newId ->
                    libraryRepository.addVideoToPlaylist(
                        playlistId = newId,
                        videoUrl = url,
                        title = stream.title,
                        thumbnail = stream.thumbnailUrl,
                        duration = stream.durationSeconds,
                        channelName = stream.uploaderName,
                        channelUrl = stream.uploaderUrl,
                        channelAvatarUrl = stream.uploaderAvatarUrl,
                        viewCount = stream.viewCount,
                    ).fold(
                        onSuccess = {
                            _state.update {
                                it.copy(
                                    playlistActionInFlight = false,
                                    playlistPickerVisible = false,
                                )
                            }
                            _events.send(PlayerEvent.AddedToPlaylist(cleanedName))
                        },
                        onFailure = {
                            _state.update { it.copy(playlistActionInFlight = false) }
                            _events.send(PlayerEvent.ActionFailed(it.message ?: ""))
                        },
                    )
                },
                onFailure = {
                    _state.update { it.copy(playlistActionInFlight = false) }
                    _events.send(PlayerEvent.ActionFailed(it.message ?: ""))
                },
            )
        }
    }

    private fun currentUrl(): String? = playerHostController.state.value.videoUrl

    private fun loadStream(url: String) {
        loadStreamJob?.cancel()
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        loadStreamJob = viewModelScope.launch {
            streamRepository.loadStream(url).fold(
                onSuccess = { stream ->
                    if (currentUrl() == url) {
                        _state.update { it.copy(isLoading = false, stream = stream) }
                        postHistory(url, stream)
                        cacheStreamMeta(url, stream)
                    }
                },
                onFailure = { throwable ->
                    if (currentUrl() == url) {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = throwable.message ?: "Could not load stream",
                            )
                        }
                    }
                },
            )
        }
    }

    private fun postHistory(url: String, stream: Stream) {
        viewModelScope.launch {
            libraryRepository.addHistory(
                videoUrl = url,
                title = stream.title,
                thumbnail = stream.thumbnailUrl,
                duration = stream.durationSeconds,
                channelName = stream.uploaderName,
                channelUrl = stream.uploaderUrl,
                channelAvatarUrl = stream.uploaderAvatarUrl,
            )
        }
    }

    private fun cacheStreamMeta(url: String, stream: Stream) {
        viewModelScope.launch {
            videoMetaRepository.put(
                VideoMeta(
                    videoUrl = url,
                    channelName = stream.uploaderName,
                    channelUrl = stream.uploaderUrl,
                    channelAvatarUrl = stream.uploaderAvatarUrl,
                    viewCount = stream.viewCount,
                ),
            )
            videoMetaRepository.cacheVideos(stream.relatedStreams)
        }
    }

    private fun toggleFavorite() {
        val url = currentUrl() ?: return
        val favorited = _state.value.isFavorited
        val stream = _state.value.stream
        val title = stream?.title.orEmpty()
        viewModelScope.launch {
            val result = if (favorited) {
                libraryRepository.removeFavorite(url)
            } else {
                libraryRepository.addFavorite(
                    videoUrl = url,
                    title = title,
                    thumbnail = stream?.thumbnailUrl.orEmpty(),
                    duration = stream?.durationSeconds ?: 0L,
                    channelName = stream?.uploaderName.orEmpty(),
                    channelUrl = stream?.uploaderUrl.orEmpty(),
                    channelAvatarUrl = stream?.uploaderAvatarUrl.orEmpty(),
                    viewCount = stream?.viewCount ?: 0L,
                )
            }
            result.fold(
                onSuccess = {
                    _events.send(if (favorited) PlayerEvent.FavoriteRemoved else PlayerEvent.FavoriteAdded(title))
                },
                onFailure = { _events.send(PlayerEvent.ActionFailed(it.message ?: "")) },
            )
        }
    }

    private fun toggleWatchLater() {
        val url = currentUrl() ?: return
        val inWatchLater = _state.value.isInWatchLater
        val stream = _state.value.stream ?: return
        viewModelScope.launch {
            val result = if (inWatchLater) {
                libraryRepository.removeWatchLater(url)
            } else {
                libraryRepository.addWatchLater(
                    url = url,
                    title = stream.title,
                    thumbnail = stream.thumbnailUrl,
                    duration = stream.durationSeconds,
                    channelName = stream.uploaderName,
                    channelUrl = stream.uploaderUrl,
                    channelAvatarUrl = stream.uploaderAvatarUrl,
                    viewCount = stream.viewCount,
                )
            }
            result.fold(
                onSuccess = {
                    _events.send(
                        if (inWatchLater) PlayerEvent.WatchLaterRemoved
                        else PlayerEvent.WatchLaterAdded(stream.title),
                    )
                },
                onFailure = { _events.send(PlayerEvent.ActionFailed(it.message ?: "")) },
            )
        }
    }
}
