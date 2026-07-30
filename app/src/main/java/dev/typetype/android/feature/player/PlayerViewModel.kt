package dev.typetype.android.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.domain.comments.CommentsRepository
import dev.typetype.android.domain.download.DownloadProgress
import dev.typetype.android.domain.download.DownloadRepository
import dev.typetype.android.domain.download.DownloadSelection
import dev.typetype.android.domain.library.LibraryRepository
import dev.typetype.android.domain.preferences.PreferencesRepository
import dev.typetype.android.domain.stream.Stream
import dev.typetype.android.domain.usersettings.UserSettingsRepository
import dev.typetype.android.feature.player.components.PlayerGestureConfig
import dev.typetype.android.feature.player.error.classifyStreamError
import dev.typetype.android.feature.player.host.PlayerHostController
import dev.typetype.android.services.PlaybackQueueCoordinator
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val playerStreamLoader: PlayerStreamLoader,
    private val playerLibraryActions: PlayerLibraryActions,
    private val preferencesRepository: PreferencesRepository,
    private val userSettingsRepository: UserSettingsRepository,
    private val playerHostController: PlayerHostController,
    private val downloadRepository: DownloadRepository,
    sabrPlaybackFactory: PlayerSabrPlaybackFactory,
    private val playbackQueueCoordinator: PlaybackQueueCoordinator,
    val commentsRepository: CommentsRepository,
    val subtitleCueLoader: PlayerSubtitleCueLoader,
) : ViewModel() {
    private val videoUrlFlow = combine(
        playerHostController.state.map { it.videoUrl },
        playbackQueueCoordinator.state,
    ) { hostUrl, queue -> queue.current?.videoUrl ?: hostUrl }
        .distinctUntilChanged()

    private val _state = MutableStateFlow(PlayerState())
    val state = _state.asStateFlow()
    internal val sabrPlayback = sabrPlaybackFactory.create(
        onFailure = { stream, failure ->
            val current = _state.value.stream
            if (current?.id == stream.id && current.requestScope == stream.requestScope) {
                _state.update { it.copy(error = classifyStreamError(failure)) }
            }
        },
    )

    private val _events = Channel<PlayerEvent>(Channel.BUFFERED)
    val events: Flow<PlayerEvent> = _events.receiveAsFlow()
    val comments = playerCommentsFlow(commentsRepository, videoUrlFlow, viewModelScope)

    private var loadStreamJob: Job? = null
    private var favoriteJob: Job? = null
    private var watchLaterJob: Job? = null

    init {
        viewModelScope.launch {
            videoUrlFlow.collect { url ->
                val hostState = playerHostController.state.value
                _state.update {
                    it.copy(
                        videoUrl = url.orEmpty(),
                        stream = null,
                        resumeAtMillis = hostState.resumePositionMillis ?: 0L,
                        initialPlayWhenReady = hostState.initialPlayWhenReady,
                        isLoading = !url.isNullOrBlank(),
                        error = null,
                        isFavorited = false,
                        isInWatchLater = false,
                        downloadInFlight = false,
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
        observeUserSettings()
        viewModelScope.launch {
            playbackQueueCoordinator.state.collect { queue ->
                _state.update { it.copy(playbackQueue = queue) }
                }
        }
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
                        autoplayCountdownSeconds = prefs.playerAutoplayCountdownSeconds,
                    )
                }
            }
        }
    }

    private fun observeUserSettings() {
        viewModelScope.launch {
            userSettingsRepository.observe().collect { settings ->
                _state.update {
                    it.copy(
                        autoplayEnabled = settings.autoplay,
                        defaultQuality = settings.defaultQuality,
                        defaultAudioLanguage = settings.defaultAudioLanguage,
                        subtitlesEnabled = settings.subtitlesEnabled,
                        defaultSubtitleLanguage = settings.defaultSubtitleLanguage,
                        preferOriginalLanguage = settings.preferOriginalLanguage,
                    )
                }
            }
        }
        viewModelScope.launch { userSettingsRepository.refresh() }
    }

    fun onAction(action: PlayerAction) {
        when (action) {
            PlayerAction.OnToggleFavorite -> toggleFavorite()
            PlayerAction.OnToggleWatchLater -> toggleWatchLater()
            PlayerAction.OnRetry -> if (_state.value.stream == null) currentUrl()?.let(::loadStream) else _state.update(PlayerState::retryPlayback)
            PlayerAction.OnAdvanceQueue -> playbackQueueCoordinator.playAutoplayNow()
            PlayerAction.OnCancelQueueAutoplay -> playbackQueueCoordinator.cancelAutoplay()
            PlayerAction.OnToggleQueueAutoplayPause ->
                playbackQueueCoordinator.toggleAutoplayPause()
            is PlayerAction.OnDownload -> downloadCurrentVideo(action.selection)
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
            playerLibraryActions.addToPlaylist(playlistId, url, stream).fold(
                onSuccess = {
                    _state.update {
                        it.copy(playlistActionInFlight = false, playlistPickerVisible = false)
                    }
                    _events.send(PlayerEvent.AddedToPlaylist(playlistName))
                },
                onFailure = {
                    _state.update { it.copy(playlistActionInFlight = false) }
                    _events.send(PlayerEvent.ActionFailed)
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
            playerLibraryActions.createPlaylistAndAdd(cleanedName, url, stream).fold(
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
                    _events.send(PlayerEvent.ActionFailed)
                },
            )
        }
    }

    private fun currentUrl(): String? = playbackQueueCoordinator.state.value.current?.videoUrl
        ?: playerHostController.state.value.videoUrl

    private fun downloadCurrentVideo(selection: DownloadSelection) {
        val url = currentUrl() ?: return
        val stream = _state.value.stream ?: return
        if (_state.value.downloadInFlight) return
        viewModelScope.launch {
            _state.update { if (it.videoUrl == url) it.copy(downloadInFlight = true) else it }
            var queuedSent = false
            runCatching {
                downloadRepository.downloadVideo(
                    videoUrl = url,
                    title = stream.title,
                    selection = selection,
                ).collect { progress ->
                    when (progress) {
                        is DownloadProgress.Queued -> {
                            if (!queuedSent) {
                                queuedSent = true
                                _events.send(PlayerEvent.DownloadQueued(progress.cached))
                            }
                        }
                        is DownloadProgress.Running -> Unit
                        is DownloadProgress.Enqueued ->
                            _events.send(PlayerEvent.DownloadEnqueued(progress.fileName))
                    }
                }
            }.onFailure {
                _events.send(PlayerEvent.DownloadFailed)
            }
            _state.update { if (it.videoUrl == url) it.copy(downloadInFlight = false) else it }
        }
    }

    private fun loadStream(url: String) {
        loadStreamJob?.cancel()
        _state.update { it.copy(isLoading = true, error = null) }
        loadStreamJob = viewModelScope.launch {
            playerStreamLoader.load(url).collect { update ->
                if (currentUrl() != url) return@collect
                _state.update { it.applyStreamUpdate(update, playerHostController.state.value) }
                when (update) {
                    is PlayerStreamUpdate.PlaybackReady ->
                        launch { playerStreamLoader.record(url, update.loaded.stream) }
                    is PlayerStreamUpdate.MetadataEnriched ->
                        launch { playerStreamLoader.cacheMetadata(url, update.stream) }
                    is PlayerStreamUpdate.Failed -> Unit
                }
            }
        }
    }

    private fun toggleFavorite() {
        val url = currentUrl() ?: return
        val favorited = _state.value.isFavorited
        val stream = _state.value.stream
        val title = stream?.title.orEmpty()
        viewModelScope.launch {
            val result = playerLibraryActions.toggleFavorite(url, stream, favorited)
            result.fold(
                onSuccess = {
                    _events.send(if (favorited) PlayerEvent.FavoriteRemoved else PlayerEvent.FavoriteAdded(title))
                },
                onFailure = { _events.send(PlayerEvent.ActionFailed) },
            )
        }
    }

    private fun toggleWatchLater() {
        val url = currentUrl() ?: return
        val inWatchLater = _state.value.isInWatchLater
        val stream = _state.value.stream ?: return
        viewModelScope.launch {
            val result = playerLibraryActions.toggleWatchLater(url, stream, inWatchLater)
            result.fold(
                onSuccess = {
                    _events.send(
                        if (inWatchLater) PlayerEvent.WatchLaterRemoved
                        else PlayerEvent.WatchLaterAdded(stream.title),
                    )
                },
                onFailure = { _events.send(PlayerEvent.ActionFailed) },
            )
        }
    }
}
