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

private const val PLAYLIST_NAME_PLACEHOLDER = "__PLAYLIST_NAME__"

@Composable
fun PlayerRoute(
    onNavigateBack: () -> Unit,
    onPlayVideo: (videoUrl: String) -> Unit,
    onOpenChannel: (channelUrl: String) -> Unit = {},
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PlayerScreen(
        state = state,
        commentsFlow = viewModel.comments,
        eventsFlow = viewModel.events,
        commentsRepository = viewModel.commentsRepository,
        onNavigateBack = onNavigateBack,
        onPlayVideo = onPlayVideo,
        onOpenChannel = onOpenChannel,
        onAction = viewModel::onAction,
    )
}

@Composable
fun PlayerScreen(
    state: PlayerState,
    commentsFlow: Flow<PagingData<Comment>>,
    eventsFlow: Flow<PlayerEvent> = emptyFlow(),
    commentsRepository: dev.typetype.android.domain.comments.CommentsRepository? = null,
    onNavigateBack: () -> Unit,
    onPlayVideo: (videoUrl: String) -> Unit,
    onOpenChannel: (channelUrl: String) -> Unit = {},
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
                is PlayerEvent.ActionFailed -> event.message
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
                state.errorMessage != null -> ErrorState(
                    message = state.errorMessage,
                    onNavigateBack = onNavigateBack,
                    onRetry = { onAction(PlayerAction.OnRetry) },
                )
                state.stream != null -> LoadedPlayer(
                    stream = state.stream,
                    videoUrl = state.videoUrl,
                    resumeAtMillis = state.resumeAtMillis,
                    isFavorited = state.isFavorited,
                    isInWatchLater = state.isInWatchLater,
                    gestureConfig = state.gestureConfig,
                    autoplayEnabled = state.autoplayEnabled,
                    defaultQuality = state.defaultQuality,
                    defaultAudioLanguage = state.defaultAudioLanguage,
                    subtitlesEnabled = state.subtitlesEnabled,
                    defaultSubtitleLanguage = state.defaultSubtitleLanguage,
                    preferOriginalLanguage = state.preferOriginalLanguage,
                    playlists = state.playlists,
                    playlistPickerVisible = state.playlistPickerVisible,
                    playlistActionInFlight = state.playlistActionInFlight,
                    downloadInFlight = state.downloadInFlight,
                    commentsFlow = commentsFlow,
                    commentsRepository = commentsRepository,
                    onNavigateBack = onNavigateBack,
                    onPlayVideo = onPlayVideo,
                    onOpenChannel = onOpenChannel,
                    onAction = onAction,
                )
            }
        }
    }
}
