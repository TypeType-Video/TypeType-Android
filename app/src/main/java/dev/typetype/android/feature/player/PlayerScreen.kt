package dev.typetype.android.feature.player

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.PagingData
import dev.typetype.android.R
import dev.typetype.android.domain.comments.Comment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.merge

private const val PLAYLIST_NAME_PLACEHOLDER = "__PLAYLIST_NAME__"

@Composable
fun PlayerRoute(
    isFullscreen: Boolean,
    onFullscreenChange: (Boolean) -> Unit,
    onNavigateBack: () -> Unit,
    onOpenAccounts: () -> Unit,
    onPlayVideo: (videoUrl: String) -> Unit,
    onOpenChannel: (channelUrl: String) -> Unit = {},
    viewModel: PlayerViewModel = hiltViewModel(),
    channelActionsViewModel: PlayerChannelActionsViewModel = hiltViewModel(),
    danmakuViewModel: PlayerDanmakuViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val channelState by channelActionsViewModel.state.collectAsStateWithLifecycle()
    val danmakuState by danmakuViewModel.state.collectAsStateWithLifecycle()
    val playerEvents = remember(viewModel.events, channelActionsViewModel.events) {
        merge(viewModel.events, channelActionsViewModel.events)
    }
    val channelUrl = state.stream?.uploaderUrl.orEmpty()
    PlayerScreen(
        state = state,
        commentsFlow = viewModel.comments,
        eventsFlow = playerEvents,
        commentsRepository = viewModel.commentsRepository,
        prepareSabrPlayback = viewModel.sabrPlayback::prepare,
        loadSubtitleCues = viewModel.subtitleCueLoader::load,
        isFullscreen = isFullscreen,
        onFullscreenChange = onFullscreenChange,
        onNavigateBack = onNavigateBack,
        onOpenAccounts = onOpenAccounts,
        onPlayVideo = onPlayVideo,
        onOpenChannel = onOpenChannel,
        isSubscribed = channelState.isSubscribed(channelUrl),
        subscriptionInFlight = channelState.isUpdating(channelUrl),
        onToggleSubscription = {
            state.stream?.let(channelActionsViewModel::toggle)
        },
        danmakuState = danmakuState,
        onDanmakuAction = danmakuViewModel::onAction,
        onAction = viewModel::onAction,
    )
}

@Composable
fun PlayerScreen(
    state: PlayerState,
    commentsFlow: Flow<PagingData<Comment>>,
    eventsFlow: Flow<PlayerEvent> = emptyFlow(),
    commentsRepository: dev.typetype.android.domain.comments.CommentsRepository? = null,
    prepareSabrPlayback: PrepareSabrPlayback = { _, _, _ -> null },
    loadSubtitleCues: LoadSubtitleCues = { Result.success(emptyList()) },
    isFullscreen: Boolean = false,
    onFullscreenChange: (Boolean) -> Unit = {},
    onNavigateBack: () -> Unit,
    onOpenAccounts: () -> Unit,
    onPlayVideo: (videoUrl: String) -> Unit,
    onOpenChannel: (channelUrl: String) -> Unit = {},
    isSubscribed: Boolean = false,
    subscriptionInFlight: Boolean = false,
    onToggleSubscription: () -> Unit = {},
    danmakuState: PlayerDanmakuState = PlayerDanmakuState(),
    onDanmakuAction: (PlayerDanmakuAction) -> Unit = {},
    onAction: (PlayerAction) -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val favoriteAdded = stringResource(R.string.player_snackbar_favorite_added)
    val favoriteRemoved = stringResource(R.string.player_snackbar_favorite_removed)
    val watchLaterAdded = stringResource(R.string.player_snackbar_watch_later_added)
    val watchLaterRemoved = stringResource(R.string.player_snackbar_watch_later_removed)
    val downloadQueued = stringResource(R.string.player_snackbar_download_queued)
    val downloadCached = stringResource(R.string.player_snackbar_download_cached)
    val downloadEnqueued = stringResource(R.string.player_snackbar_download_enqueued)
    val downloadFailed = stringResource(R.string.player_snackbar_download_failed)
    val actionFailed = stringResource(R.string.snackbar_action_failed)
    val addedToPlaylist = stringResource(
        R.string.player_snackbar_added_to_playlist,
        PLAYLIST_NAME_PLACEHOLDER,
    )
    LaunchedEffect(
        eventsFlow,
        favoriteAdded,
        favoriteRemoved,
        watchLaterAdded,
        watchLaterRemoved,
        downloadQueued,
        downloadCached,
        downloadEnqueued,
        downloadFailed,
        actionFailed,
        addedToPlaylist,
    ) {
        eventsFlow.collect { event ->
            val message = when (event) {
                is PlayerEvent.FavoriteAdded -> favoriteAdded
                PlayerEvent.FavoriteRemoved -> favoriteRemoved
                is PlayerEvent.WatchLaterAdded -> watchLaterAdded
                PlayerEvent.WatchLaterRemoved -> watchLaterRemoved
                is PlayerEvent.AddedToPlaylist ->
                    addedToPlaylist.replace(PLAYLIST_NAME_PLACEHOLDER, event.playlistName)
                is PlayerEvent.DownloadQueued -> if (event.cached) downloadCached else downloadQueued
                is PlayerEvent.DownloadEnqueued -> downloadEnqueued
                PlayerEvent.DownloadFailed -> downloadFailed
                PlayerEvent.ActionFailed -> actionFailed
            }
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
        }
    }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
    ) { padding ->
        Surface(
            modifier = Modifier.fillMaxSize().padding(padding),
            color = MaterialTheme.colorScheme.background,
        ) {
            when {
                state.isLoading -> LoadingState()
                state.error != null -> ErrorState(
                    classification = state.error,
                    onNavigateBack = onNavigateBack,
                    onRetry = { onAction(PlayerAction.OnRetry) },
                    onOpenAccounts = onOpenAccounts,
                )
                state.stream != null -> LoadedPlayer(
                    stream = state.stream,
                    videoUrl = state.videoUrl,
                    resumeAtMillis = state.resumeAtMillis,
                    initialPlayWhenReady = state.initialPlayWhenReady,
                    playbackBindGeneration = state.playbackBindGeneration,
                    isFavorited = state.isFavorited,
                    isInWatchLater = state.isInWatchLater,
                    gestureConfig = state.gestureConfig,
                    autoplayCountdownSeconds = state.autoplayCountdownSeconds,
                    userSettings = state.userSettings,
                    playlists = state.playlists,
                    playlistPickerVisible = state.playlistPickerVisible,
                    playlistActionInFlight = state.playlistActionInFlight,
                    downloadInFlight = state.downloadInFlight,
                    playbackQueue = state.playbackQueue,
                    commentsFlow = commentsFlow,
                    commentsRepository = commentsRepository,
                    prepareSabrPlayback = prepareSabrPlayback,
                    loadSubtitleCues = loadSubtitleCues,
                    isFullscreen = isFullscreen,
                    onFullscreenChange = onFullscreenChange,
                    onNavigateBack = onNavigateBack,
                    onOpenAccounts = onOpenAccounts,
                    onPlayVideo = onPlayVideo,
                    onOpenChannel = onOpenChannel,
                    isSubscribed = isSubscribed,
                    subscriptionInFlight = subscriptionInFlight,
                    onToggleSubscription = onToggleSubscription,
                    danmakuState = danmakuState,
                    onDanmakuAction = onDanmakuAction,
                    onAction = onAction,
                )
            }
        }
    }
}
