package dev.typetype.android.feature.shorts

import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.LocalActivity
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import dev.typetype.android.R
import dev.typetype.android.core.ui.copyPlainText
import dev.typetype.android.core.ui.components.LocalAppSnackbarHost
import dev.typetype.android.feature.menu.rememberVideoMenuScope
import dev.typetype.android.feature.player.PlayerChannelActionsViewModel
import dev.typetype.android.feature.player.DevicePlaybackCodecSupport
import dev.typetype.android.feature.player.PlayerViewModel
import dev.typetype.android.feature.player.PlayerFullscreenEffect
import dev.typetype.android.feature.player.ShortsPlayerRoute
import dev.typetype.android.feature.player.components.CommentsSheet
import dev.typetype.android.feature.player.components.LocalMediaController
import dev.typetype.android.feature.player.components.rememberCurrentMediaId
import dev.typetype.android.feature.player.components.rememberPlayerPlaybackStatus
import dev.typetype.android.feature.player.host.PlayerHostController
import dev.typetype.android.feature.player.host.PlayerHostTarget
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ShortsRoute(
    onNavigateBack: () -> Unit,
    onPlayVideo: (String) -> Unit,
    onOpenChannel: (String) -> Unit,
    playerHostController: PlayerHostController,
    playerViewModel: PlayerViewModel,
    viewModel: ShortsViewModel = hiltViewModel(),
    channelActionsViewModel: PlayerChannelActionsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val playerState by playerViewModel.state.collectAsStateWithLifecycle()
    val channelState by channelActionsViewModel.state.collectAsStateWithLifecycle()
    val playerHostState by playerHostController.state.collectAsStateWithLifecycle()
    val mediaController = LocalMediaController.current
    val currentMediaId = rememberCurrentMediaId(mediaController)
    val playbackStatus = mediaController?.let { rememberPlayerPlaybackStatus(it) }
    val menuScope = rememberVideoMenuScope(onOpenChannel)
    val visibleState = state.copy(videos = state.videos.filterNot(menuScope::isHidden))
    val snackbarHost = LocalAppSnackbarHost.current
    val context = LocalContext.current
    val codecSupport = remember(context.applicationContext) {
        DevicePlaybackCodecSupport(context.applicationContext)
    }
    val activity = LocalActivity.current
    val actionFailed = stringResource(R.string.snackbar_action_failed)
    val titleCopied = stringResource(R.string.shorts_title_copied)
    val coroutineScope = rememberCoroutineScope()
    var commentsVideoUrl by remember { mutableStateOf<String?>(null) }
    val playbackReady = playerHostState.target == PlayerHostTarget.Embedded &&
        currentMediaId == playerHostState.videoUrl &&
        playbackStatus?.playbackState == Player.STATE_READY

    PlayerFullscreenEffect(
        activity = activity,
        isFullscreen = true,
        locksLandscape = false,
    )

    LaunchedEffect(channelActionsViewModel, snackbarHost, actionFailed) {
        channelActionsViewModel.events.collect {
            snackbarHost?.showSnackbar(actionFailed, duration = SnackbarDuration.Short)
        }
    }

    DisposableEffect(playerHostController) {
        onDispose { playerHostController.closeEmbeddedPlayback() }
    }
    LaunchedEffect(visibleState.hidden, visibleState.videos.isEmpty()) {
        if (visibleState.hidden || visibleState.videos.isEmpty()) {
            playerHostController.closeEmbeddedPlayback()
        }
    }

    ShortsScreen(
        state = visibleState,
        onNavigateBack = onNavigateBack,
        onPlayVideo = { url ->
            if (
                playerHostState.videoUrl == url &&
                playerHostState.target == PlayerHostTarget.Embedded
            ) {
                playerHostController.expand()
            } else {
                onPlayVideo(url)
            }
        },
        onOpenChannel = { feedChannelUrl ->
            val playbackChannelUrl = playerState.stream?.uploaderUrl?.takeIf {
                playerState.videoUrl == playerHostState.videoUrl && it.isNotBlank()
            }
            (playbackChannelUrl ?: feedChannelUrl.takeIf(String::isNotBlank))?.let(onOpenChannel)
        },
        onRefresh = { viewModel.onAction(ShortsAction.Refresh) },
        onLoadMore = { viewModel.onAction(ShortsAction.LoadMore) },
        menuItemState = menuScope::stateFor,
        onMenuAction = { action, video -> menuScope.onAction(action, video) },
        onShowComments = if (playerState.userSettings.hideComments) null else {
            { video -> commentsVideoUrl = video.url }
        },
        onCopyTitle = { title ->
            copyPlainText(
                context = context,
                value = title,
                labelRes = R.string.shorts_title_clipboard_label,
                confirmationRes = R.string.shorts_title_copied,
            )
            coroutineScope.launch {
                snackbarHost?.showSnackbar(titleCopied, duration = SnackbarDuration.Short)
            }
        },
        isSubscribed = { channelState.isSubscribed(it.uploaderUrl) },
        subscriptionInFlight = { channelState.isUpdating(it.uploaderUrl) },
        onToggleSubscription = { video ->
            channelActionsViewModel.toggle(
                video.uploaderUrl,
                video.uploaderName,
                video.uploaderAvatarUrl,
            )
        },
        embeddedPlaybackEnabled = true,
        playbackReady = playbackReady,
        onActiveVideoChanged = { video ->
            if (video == null) {
                mediaController?.pause()
            } else {
                if (playerHostController.state.value.videoUrl != video.url) {
                    mediaController?.pause()
                }
                playerHostController.openEmbeddedVideo(
                    url = video.url,
                    autoplay = state.autoplayEnabled,
                    returnPositionMillis = mediaController?.currentPosition?.coerceAtLeast(0L),
                    returnPlayWhenReady = mediaController?.playWhenReady == true,
                )
            }
        },
        onUpcomingVideosChanged = { videos ->
            if (context.allowsShortsPlaybackPrefetch()) {
                videos.forEachIndexed { index, video ->
                    if (index > 0) delay(SHORTS_SECONDARY_PREFETCH_DELAY_MILLIS)
                    viewModel.preheatPlayback(
                        videoUrl = video.url,
                        settings = playerState.userSettings,
                        codecSupport = codecSupport,
                        prepareSession = index == 0,
                    )
                }
            }
        },
        statsForVideo = { video ->
            val stream = playerState.stream.takeIf { playerState.videoUrl == video.url }
            ShortsVideoStats(
                viewCount = stream?.viewCount?.takeIf { it >= 0L }
                    ?: video.viewCount.takeIf { it >= 0L },
                likeCount = stream?.likeCount?.takeIf { it >= 0L },
            )
        },
        embeddedPlayback = { video, onAdvance ->
            if (
                playerHostState.videoUrl == video.url &&
                playerHostState.target == PlayerHostTarget.Embedded
            ) {
                ShortsPlayerRoute(
                    videoUrl = video.url,
                    viewModel = playerViewModel,
                    onAdvance = onAdvance,
                )
            }
        },
    )

    commentsVideoUrl?.let { videoUrl ->
        CommentsSheet(
            pagingFlow = playerViewModel.comments,
            videoUrl = videoUrl,
            commentsRepository = playerViewModel.commentsRepository,
            onDismiss = { commentsVideoUrl = null },
            onTimestampClick = { positionMillis ->
                mediaController?.seekTo(positionMillis)
                commentsVideoUrl = null
            },
        )
    }
}

private const val SHORTS_SECONDARY_PREFETCH_DELAY_MILLIS = 750L
