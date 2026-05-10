package dev.typetype.android.feature.player

import android.content.pm.ActivityInfo
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WatchLater
import androidx.compose.material.icons.outlined.WatchLater
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.session.MediaController
import androidx.paging.PagingData
import dev.typetype.android.R
import dev.typetype.android.core.ui.util.WindowHelper
import dev.typetype.android.domain.comments.Comment
import dev.typetype.android.domain.stream.Stream
import dev.typetype.android.feature.player.components.CommentsBar
import dev.typetype.android.feature.player.components.CommentsSheet
import dev.typetype.android.feature.player.components.DescriptionSection
import dev.typetype.android.feature.player.components.LocalMediaController
import dev.typetype.android.feature.player.components.PlayerGestureConfig
import dev.typetype.android.feature.player.components.PlayerSurfaceBox
import dev.typetype.android.feature.player.components.RelatedStreamsSection
import dev.typetype.android.feature.player.components.UploaderCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

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
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(Unit) {
        eventsFlow.collect { event ->
            val message = when (event) {
                is PlayerEvent.FavoriteAdded -> context.getString(R.string.player_snackbar_favorite_added)
                PlayerEvent.FavoriteRemoved -> context.getString(R.string.player_snackbar_favorite_removed)
                is PlayerEvent.WatchLaterAdded -> context.getString(R.string.player_snackbar_watch_later_added)
                PlayerEvent.WatchLaterRemoved -> context.getString(R.string.player_snackbar_watch_later_removed)
                is PlayerEvent.AddedToPlaylist ->
                    context.getString(R.string.player_snackbar_added_to_playlist, event.playlistName)
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
                    playlists = state.playlists,
                    playlistPickerVisible = state.playlistPickerVisible,
                    playlistActionInFlight = state.playlistActionInFlight,
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

@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            dev.typetype.android.core.ui.components.AnimatedLoader(size = 88.dp)
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    onNavigateBack: () -> Unit,
    onRetry: () -> Unit,
) {
    val classification = dev.typetype.android.feature.player.error.classifyStreamError(message)
    val illustrationRes = when (classification.kind) {
        dev.typetype.android.feature.player.error.StreamErrorKind.MemberOnly,
        dev.typetype.android.feature.player.error.StreamErrorKind.GeoRestricted,
        -> R.raw.member_only
        dev.typetype.android.feature.player.error.StreamErrorKind.Generic -> R.raw.error_cat
    }
    val displayMessage = when (classification.kind) {
        dev.typetype.android.feature.player.error.StreamErrorKind.MemberOnly ->
            stringResource(R.string.state_member_only_message)
        dev.typetype.android.feature.player.error.StreamErrorKind.GeoRestricted,
        dev.typetype.android.feature.player.error.StreamErrorKind.Generic ->
            classification.rawMessage ?: stringResource(R.string.state_failed_to_load_stream)
    }
    Box(modifier = Modifier.fillMaxSize()) {
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.player_back),
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
        dev.typetype.android.core.ui.components.StreamErrorState(
            title = stringResource(R.string.state_couldnt_load_video),
            message = displayMessage,
            illustrationRes = illustrationRes,
            countryCode = classification.countryCode,
            onRetry = onRetry,
            onBack = onNavigateBack,
        )
    }
}

@Composable
private fun LoadedPlayer(
    stream: Stream,
    videoUrl: String,
    resumeAtMillis: Long,
    isFavorited: Boolean,
    isInWatchLater: Boolean,
    gestureConfig: PlayerGestureConfig,
    autoplayEnabled: Boolean,
    playlists: List<dev.typetype.android.domain.library.Playlist>,
    playlistPickerVisible: Boolean,
    playlistActionInFlight: Boolean,
    commentsFlow: Flow<PagingData<Comment>>,
    commentsRepository: dev.typetype.android.domain.comments.CommentsRepository?,
    onNavigateBack: () -> Unit,
    onPlayVideo: (videoUrl: String) -> Unit,
    onOpenChannel: (channelUrl: String) -> Unit = {},
    onAction: (PlayerAction) -> Unit = {},
) {
    val controller = LocalMediaController.current
    val scrollState = rememberScrollState()
    var commentsVisible by remember { mutableStateOf(false) }
    var isFullscreen by remember { mutableStateOf(false) }
    val activity = LocalActivity.current

    LaunchedEffect(stream.id, controller) {
        controller?.let { ctrl -> bindStreamToController(ctrl, stream, videoUrl, resumeAtMillis) }
    }

    val durationMs = stream.durationSeconds * 1000L
    val saveProgressIfEligible: (Long) -> Unit = saveProgressIfEligible@{ positionMs ->
        if (positionMs < 5_000L) return@saveProgressIfEligible
        if (durationMs > 0 && positionMs >= (durationMs * 0.95).toLong()) return@saveProgressIfEligible
        onAction(PlayerAction.OnSaveProgress(positionMs))
    }

    LaunchedEffect(controller) {
        while (true) {
            delay(10_000)
            val ctrl = controller ?: continue
            if (ctrl.isPlaying) saveProgressIfEligible(ctrl.currentPosition)
        }
    }

    DisposableEffect(controller, autoplayEnabled, stream.relatedStreams) {
        val ctrl = controller
        if (ctrl == null) {
            onDispose { }
        } else {
            val nextUrl = stream.relatedStreams.firstOrNull()?.url
            val listener = object : androidx.media3.common.Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == androidx.media3.common.Player.STATE_ENDED &&
                        autoplayEnabled &&
                        !nextUrl.isNullOrBlank()
                    ) {
                        onPlayVideo(nextUrl)
                    }
                }
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    dev.typetype.android.feature.player.components.applyAutoEnterPipParams(activity, isPlaying)
                    if (!isPlaying) saveProgressIfEligible(ctrl.currentPosition)
                }
                override fun onPositionDiscontinuity(
                    oldPosition: androidx.media3.common.Player.PositionInfo,
                    newPosition: androidx.media3.common.Player.PositionInfo,
                    reason: Int,
                ) {
                    if (reason == androidx.media3.common.Player.DISCONTINUITY_REASON_SEEK) {
                        saveProgressIfEligible(newPosition.positionMs)
                    }
                }
            }
            ctrl.addListener(listener)
            dev.typetype.android.feature.player.components.applyAutoEnterPipParams(activity, ctrl.isPlaying)
            onDispose {
                ctrl.removeListener(listener)
                dev.typetype.android.feature.player.components.applyAutoEnterPipParams(activity, false)
            }
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, controller) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE) {
                controller?.let { saveProgressIfEligible(it.currentPosition) }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(isFullscreen) {
        val window = activity?.window ?: return@LaunchedEffect
        if (isFullscreen) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            WindowHelper.toggleFullscreen(window, isFullscreen = true)
        } else {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            WindowHelper.toggleFullscreen(window, isFullscreen = false)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            controller?.let { ctrl -> saveProgressIfEligible(ctrl.currentPosition) }
            val window = activity?.window ?: return@onDispose
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            WindowHelper.toggleFullscreen(window, isFullscreen = false)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .then(
                if (isFullscreen) Modifier
                else Modifier.windowInsetsPadding(WindowInsets.statusBars),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (isFullscreen) Modifier else Modifier.verticalScroll(scrollState)),
        ) {
            Box(
                modifier = if (isFullscreen) {
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                } else {
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(Color.Black)
                },
                contentAlignment = Alignment.Center,
            ) {
                if (controller != null) {
                    PlayerSurfaceBox(
                        player = controller,
                        onNavigateBack = {
                            if (isFullscreen) isFullscreen = false else onNavigateBack()
                        },
                        isFullscreen = isFullscreen,
                        onToggleFullscreen = { isFullscreen = !isFullscreen },
                        sponsorBlockSegments = stream.sponsorBlockSegments,
                        chapters = stream.chapters,
                        gestureConfig = gestureConfig,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            if (!isFullscreen) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    DescriptionSection(
                        title = stream.title,
                        viewCount = stream.viewCount,
                        likeCount = stream.likeCount,
                        description = stream.description,
                    )
                    PlayerInteractionRow(
                        isFavorited = isFavorited,
                        isInWatchLater = isInWatchLater,
                        shareUrl = videoUrl,
                        onToggleFavorite = { onAction(PlayerAction.OnToggleFavorite) },
                        onToggleWatchLater = { onAction(PlayerAction.OnToggleWatchLater) },
                        onAddToPlaylist = { onAction(PlayerAction.OnOpenPlaylistPicker) },
                    )
                    UploaderCard(
                        name = stream.uploaderName,
                        avatarUrl = stream.uploaderAvatarUrl,
                        subscriberCount = stream.uploaderSubscriberCount,
                        verified = stream.uploaderVerified,
                        onCardClick = { onOpenChannel(stream.uploaderUrl) },
                    )
                    CommentsBar(onClick = { commentsVisible = true })
                    Spacer(Modifier.height(4.dp))
                    RelatedStreamsSection(
                        videos = stream.relatedStreams,
                        onPlayVideo = onPlayVideo,
                    )
                }
            }
        }
    }

    if (commentsVisible && commentsRepository != null) {
        CommentsSheet(
            pagingFlow = commentsFlow,
            videoUrl = videoUrl,
            commentsRepository = commentsRepository,
            onDismiss = { commentsVisible = false },
        )
    }

    if (playlistPickerVisible) {
        dev.typetype.android.feature.player.components.PlaylistPickerSheet(
            playlists = playlists,
            isInFlight = playlistActionInFlight,
            onAddToPlaylist = { onAction(PlayerAction.OnAddToPlaylist(it)) },
            onCreatePlaylist = { onAction(PlayerAction.OnCreatePlaylistAndAdd(it)) },
            onDismiss = { onAction(PlayerAction.OnDismissPlaylistPicker) },
        )
    }
}

@Composable
private fun PlayerInteractionRow(
    isFavorited: Boolean,
    isInWatchLater: Boolean,
    shareUrl: String,
    onToggleFavorite: () -> Unit,
    onToggleWatchLater: () -> Unit,
    onAddToPlaylist: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val shareChooserTitle = stringResource(R.string.video_menu_share_chooser)
    val serverBaseUrl = dev.typetype.android.core.ui.share.LocalServerBaseUrl.current
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        IconButton(onClick = onToggleFavorite) {
            Icon(
                imageVector = if (isFavorited) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = stringResource(
                    if (isFavorited) R.string.player_remove_from_favorites
                    else R.string.player_add_to_favorites,
                ),
                tint = if (isFavorited) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
            )
        }
        IconButton(onClick = onToggleWatchLater) {
            Icon(
                imageVector = if (isInWatchLater) Icons.Filled.WatchLater else Icons.Outlined.WatchLater,
                contentDescription = stringResource(
                    if (isInWatchLater) R.string.player_remove_from_watch_later
                    else R.string.player_add_to_watch_later,
                ),
                tint = if (isInWatchLater) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
            )
        }
        IconButton(onClick = onAddToPlaylist) {
            Icon(
                imageVector = Icons.Filled.PlaylistAdd,
                contentDescription = stringResource(R.string.player_add_to_playlist),
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
        IconButton(onClick = {
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(
                    android.content.Intent.EXTRA_TEXT,
                    dev.typetype.android.core.ui.share.buildShareUrl(serverBaseUrl, shareUrl),
                )
            }
            context.startActivity(android.content.Intent.createChooser(intent, shareChooserTitle))
        }) {
            Icon(
                imageVector = Icons.Filled.Share,
                contentDescription = stringResource(R.string.video_menu_share),
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

