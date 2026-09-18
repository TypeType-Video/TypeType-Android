@file:androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])

package video.typetype.tv.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusEnterExitScope
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import video.typetype.sdk.core.PlaybackSession
import video.typetype.sdk.core.StreamDetails
import video.typetype.sdk.core.Video
import video.typetype.sdk.core.SubtitleTrack
import video.typetype.sdk.core.Comment
import video.typetype.sdk.core.AudioOnlyStream
import video.typetype.sdk.core.UserSettings
import video.typetype.tv.player.TypeTypePlaybackService
import video.typetype.tv.player.SponsorBlockPolicy
import video.typetype.tv.player.skipTarget

@Composable
@OptIn(ExperimentalComposeUiApi::class)
internal fun PlayerScreen(
    video: Video,
    stream: StreamDetails,
    playback: PlaybackSession,
    audioOnly: AudioOnlyStream?,
    supportedVideoItags: Set<Int>,
    subtitle: SubtitleTrack?,
    commentsState: CommentsUiState,
    settings: UserSettings,
    isAdvancing: Boolean,
    serverError: String?,
    onClose: () -> Unit,
    onPlayNext: () -> Unit,
    onPlayQueueItem: (Video) -> Unit,
    onSelectVideoTrack: (Int, Long) -> Unit,
    onSelectAudioTrack: (Int, String?, Long) -> Unit,
    onSelectSubtitle: (String?, Boolean, String?, Long) -> Unit,
    onLoadMoreComments: () -> Unit,
    onLoadCommentReplies: (Comment) -> Unit,
) {
    val context = LocalContext.current
    var controlsVisible by remember { mutableStateOf(true) }
    var controlsInteraction by remember { mutableLongStateOf(0L) }
    var queueRequested by remember { mutableStateOf(false) }
    var tracksRequested by remember { mutableStateOf(false) }
    var commentsRequested by remember { mutableStateOf(false) }
    val sponsorBlockPolicy = remember(stream.sponsorBlockSegments, stream.durationSeconds, settings) {
        SponsorBlockPolicy.create(stream, settings)
    }
    val screenFocus = remember { FocusRequester() }
    val playFocus = remember { FocusRequester() }
    val autoplayCountdown = rememberAutoplayCountdown(
        playback.sessionId,
        settings.autoplay && stream.relatedStreams.isNotEmpty(),
        settings.autoplayCountdownSeconds,
        onPlayNext,
    )
    val playerSession = rememberPlayerSessionBinding(
        context, video, stream, playback, subtitle, audioOnly, settings,
    ) {
        controlsVisible = !settings.autoplay || stream.relatedStreams.isEmpty()
        autoplayCountdown.onPlaybackEnded()
    }
    val player = playerSession.controller
    val positionMilliseconds = playerSession.positionMilliseconds
    val durationMilliseconds = playerSession.durationMilliseconds
    val showQueue = stream.relatedStreams.isNotEmpty() && (queueRequested || isAdvancing)
    LaunchedEffect(player, controlsVisible, queueRequested, tracksRequested, commentsRequested, autoplayCountdown.active) {
        if (player != null && !queueRequested && !tracksRequested && !commentsRequested && !autoplayCountdown.active) {
            if (controlsVisible) playFocus.requestFocus() else screenFocus.requestFocus()
        }
    }
    LaunchedEffect(
        controlsVisible, playerSession.isPlaying, queueRequested, tracksRequested, commentsRequested,
        autoplayCountdown.active, controlsInteraction,
    ) {
        if (controlsVisible && playerSession.isPlaying && !queueRequested && !tracksRequested &&
            !commentsRequested && !autoplayCountdown.active
        ) {
            delay(8_000L)
            controlsVisible = false
        }
    }
    BackHandler {
        if (autoplayCountdown.active) {
            autoplayCountdown.cancel()
            controlsVisible = true
        } else if (commentsRequested) {
            commentsRequested = false
            controlsVisible = true
        } else if (tracksRequested) {
            tracksRequested = false
        } else if (queueRequested) {
            queueRequested = false
        } else if (controlsVisible) {
            controlsVisible = false
        } else {
            TypeTypePlaybackService.stop(context)
            onClose()
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusProperties { onExit = { scope: FocusEnterExitScope -> scope.cancelFocusChange() } }
            .focusGroup()
            .background(Color.Black)
            .focusRequester(screenFocus)
            .focusable()
            .handlePlayerKeys(
                controlsVisible = controlsVisible,
                interactiveOverlayVisible = autoplayCountdown.active || commentsRequested || tracksRequested || queueRequested,
                hasQueue = stream.relatedStreams.isNotEmpty(),
                onControlsInteraction = { controlsInteraction += 1L },
                onShowQueue = { queueRequested = true },
                onSeekBack = {
                    player?.let { controller ->
                        TypeTypePlaybackService.seek(
                            context,
                            (controller.currentPosition - SEEK_INCREMENT_MILLISECONDS).coerceAtLeast(0L),
                        )
                    }
                },
                onSeekForward = {
                    player?.let { controller ->
                        val target = controller.currentPosition + SEEK_INCREMENT_MILLISECONDS
                        TypeTypePlaybackService.seek(
                            context,
                            controller.duration.takeIf { it > 0L }?.let(target::coerceAtMost) ?: target,
                        )
                    }
                },
                onShowControls = { controlsVisible = true },
            ),
        contentAlignment = Alignment.Center,
    ) {
        val playerWidth by animateFloatAsState(
            targetValue = if (commentsRequested) .66f else 1f,
            animationSpec = tween(240),
            label = "player-comments-width",
        )
        if (player == null) {
            PlayerLoading(playerSession.playbackError)
        } else {
            if (audioOnly != null) {
                AudioOnlyPlaybackBackdrop(video)
            } else {
                AndroidView(
                    factory = { viewContext ->
                        PlayerView(viewContext).apply {
                            this.player = player
                            useController = false
                            isFocusable = false
                        }
                    },
                    modifier = Modifier.fillMaxHeight().fillMaxWidth(playerWidth).align(Alignment.CenterStart),
                )
            }
            playerSession.playbackError?.let { message ->
                NoServerMedia(
                    message = message,
                    onRetry = { TypeTypePlaybackService.retry(context) },
                    onClose = {
                        TypeTypePlaybackService.stop(context)
                        onClose()
                    },
                )
            }
            PlayerOverlays(
                video = video,
                stream = stream,
                playback = playback,
                supportedVideoItags = supportedVideoItags,
                subtitle = subtitle,
                isAudioOnly = audioOnly != null,
                controlsVisible = controlsVisible,
                showQueue = showQueue,
                tracksRequested = tracksRequested,
                commentsRequested = commentsRequested,
                commentsState = commentsState,
                isPlaying = playerSession.isPlaying,
                positionMilliseconds = positionMilliseconds,
                durationMilliseconds = durationMilliseconds,
                isAdvancing = isAdvancing,
                serverError = serverError,
                subtitleError = playerSession.subtitleError,
                sponsorBlockPolicy = sponsorBlockPolicy,
                activeSponsorSegment = sponsorBlockPolicy.activeSegment(positionMilliseconds),
                playFocusRequester = playFocus,
                onSeekBack = {
                    TypeTypePlaybackService.seek(
                        context,
                        (player.currentPosition - SEEK_INCREMENT_MILLISECONDS).coerceAtLeast(0L),
                    )
                },
                onPlayPause = { if (player.isPlaying) player.pause() else player.play() },
                onSeekForward = {
                    val target = player.currentPosition + SEEK_INCREMENT_MILLISECONDS
                    TypeTypePlaybackService.seek(
                        context,
                        player.duration.takeIf { it > 0L }?.let(target::coerceAtMost) ?: target,
                    )
                },
                onQueue = { queueRequested = true },
                onTracks = { tracksRequested = true },
                onComments = {
                    controlsVisible = false
                    commentsRequested = true
                },
                onSkipSponsor = { segment ->
                    TypeTypePlaybackService.seek(
                        context,
                        sponsorBlockPolicy.skipTarget(segment, durationMilliseconds),
                    )
                },
                onLoadMoreComments = onLoadMoreComments,
                onLoadCommentReplies = onLoadCommentReplies,
                onSeekTimestamp = { position -> TypeTypePlaybackService.seek(context, position) },
                onPlayQueueItem = onPlayQueueItem,
                onSelectVideoTrack = onSelectVideoTrack,
                onSelectAudioTrack = onSelectAudioTrack,
                onSelectSubtitle = onSelectSubtitle,
                onDismissTracks = { tracksRequested = false },
                onDismissComments = {
                    commentsRequested = false
                    controlsVisible = true
                },
            )
            if (autoplayCountdown.active) {
                stream.relatedStreams.firstOrNull()?.let { next ->
                    AutoplayCountdownOverlay(
                        next = next,
                        remainingSeconds = autoplayCountdown.remainingSeconds,
                        totalSeconds = autoplayCountdown.totalSeconds,
                        paused = autoplayCountdown.paused,
                        onPlayNow = autoplayCountdown::playNow,
                        onTogglePause = autoplayCountdown::togglePause,
                        onCancel = {
                            autoplayCountdown.cancel()
                            controlsVisible = true
                        },
                    )
                }
            }
        }
    }
}

private const val SEEK_INCREMENT_MILLISECONDS = 10_000L
