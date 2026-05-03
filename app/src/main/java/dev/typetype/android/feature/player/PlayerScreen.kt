package dev.typetype.android.feature.player

import android.content.pm.ActivityInfo
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
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
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.session.MediaController
import androidx.paging.PagingData
import dev.typetype.android.R
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
                is PlayerEvent.ActionFailed -> event.message
            }
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
        }
    }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
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
                )
                state.stream != null -> LoadedPlayer(
                    stream = state.stream,
                    videoUrl = state.videoUrl,
                    isFavorited = state.isFavorited,
                    isInWatchLater = state.isInWatchLater,
                    gestureConfig = state.gestureConfig,
                    commentsFlow = commentsFlow,
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
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun ErrorState(message: String, onNavigateBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier.align(Alignment.Start),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.player_back),
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
        Spacer(Modifier.height(48.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun LoadedPlayer(
    stream: Stream,
    videoUrl: String,
    isFavorited: Boolean,
    isInWatchLater: Boolean,
    gestureConfig: PlayerGestureConfig,
    commentsFlow: Flow<PagingData<Comment>>,
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
        controller?.let { ctrl -> bindStreamToController(ctrl, stream, videoUrl) }
    }

    LaunchedEffect(controller) {
        while (true) {
            delay(5_000)
            val ctrl = controller ?: continue
            if (ctrl.isPlaying) onAction(PlayerAction.OnSaveProgress(ctrl.currentPosition))
        }
    }

    LaunchedEffect(isFullscreen) {
        val window = activity?.window ?: return@LaunchedEffect
        val ctrl = WindowCompat.getInsetsController(window, window.decorView)
        if (isFullscreen) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            ctrl.hide(WindowInsetsCompat.Type.systemBars())
            ctrl.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            ctrl.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            controller?.let { ctrl ->
                if (ctrl.currentPosition > 0) {
                    onAction(PlayerAction.OnSaveProgress(ctrl.currentPosition))
                }
            }
            val window = activity?.window ?: return@onDispose
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            WindowCompat.getInsetsController(window, window.decorView)
                .show(WindowInsetsCompat.Type.systemBars())
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black),
                contentAlignment = Alignment.Center,
            ) {
                if (controller != null) {
                    PlayerSurfaceBox(
                        player = controller,
                        onNavigateBack = onNavigateBack,
                        isFullscreen = false,
                        onToggleFullscreen = { isFullscreen = true },
                        sponsorBlockSegments = stream.sponsorBlockSegments,
                        chapters = stream.chapters,
                        gestureConfig = gestureConfig,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
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
                    onToggleFavorite = { onAction(PlayerAction.OnToggleFavorite) },
                    onToggleWatchLater = { onAction(PlayerAction.OnToggleWatchLater) },
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

        if (isFullscreen && controller != null) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                PlayerSurfaceBox(
                    player = controller,
                    onNavigateBack = { isFullscreen = false },
                    isFullscreen = true,
                    onToggleFullscreen = { isFullscreen = false },
                    sponsorBlockSegments = stream.sponsorBlockSegments,
                    chapters = stream.chapters,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    if (commentsVisible) {
        CommentsSheet(
            pagingFlow = commentsFlow,
            onDismiss = { commentsVisible = false },
        )
    }
}

@Composable
private fun PlayerInteractionRow(
    isFavorited: Boolean,
    isInWatchLater: Boolean,
    onToggleFavorite: () -> Unit,
    onToggleWatchLater: () -> Unit,
) {
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
                imageVector = if (isInWatchLater) Icons.Filled.BookmarkAdded else Icons.Filled.BookmarkAdd,
                contentDescription = stringResource(
                    if (isInWatchLater) R.string.player_remove_from_watch_later
                    else R.string.player_add_to_watch_later,
                ),
                tint = if (isInWatchLater) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

