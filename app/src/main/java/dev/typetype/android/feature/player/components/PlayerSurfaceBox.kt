package dev.typetype.android.feature.player.components

import android.media.AudioManager
import android.graphics.Rect
import androidx.activity.compose.LocalActivity
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.ui.compose.state.rememberPresentationState
import dev.typetype.android.domain.stream.Chapter
import dev.typetype.android.domain.stream.SponsorBlockSegment
import dev.typetype.android.domain.stream.Stream
import dev.typetype.android.domain.stream.StreamPlaybackContract
import dev.typetype.android.domain.usersettings.CaptionStyles
import dev.typetype.android.feature.player.LoadSubtitleCues
import dev.typetype.android.feature.player.PlaybackCodecSupport
import dev.typetype.android.feature.player.PlayerDanmakuAction
import dev.typetype.android.feature.player.PlayerDanmakuState
import dev.typetype.android.feature.player.SponsorBlockPlaybackPolicy
import dev.typetype.android.feature.player.key
import dev.typetype.android.feature.player.state.PlayerGestureState
import dev.typetype.android.feature.player.state.DragMode
import dev.typetype.android.feature.player.state.ResizeMode
import dev.typetype.android.feature.player.state.next
import kotlinx.coroutines.delay

private const val AUTO_HIDE_DELAY_MS = 3_500L

@OptIn(markerClass = [UnstableApi::class])
@Composable
internal fun PlayerSurfaceBox(
    player: MediaController,
    stream: Stream,
    audioOnlyState: AudioOnlyPlaybackState,
    selectedCodec: String,
    selectedQuality: String,
    selectedAudioKey: String?,
    selectedSubtitleKey: String?,
    selectedSpeed: Float,
    codecSupport: PlaybackCodecSupport,
    onSelectCodec: (String) -> Unit,
    onSelectQuality: (String) -> Unit,
    onSelectAudio: (String?) -> Unit,
    onSelectSubtitle: (String?) -> Unit,
    onSelectSpeed: (Float) -> Unit,
    onNavigateBack: () -> Unit,
    onRetryPlayback: () -> Unit,
    onOpenAccounts: () -> Unit,
    modifier: Modifier = Modifier,
    pipSourceRect: Rect? = null,
    isFullscreen: Boolean = false,
    onToggleFullscreen: () -> Unit = {},
    sponsorBlockPolicy: SponsorBlockPlaybackPolicy = SponsorBlockPlaybackPolicy(),
    chapters: List<Chapter> = emptyList(),
    gestureConfig: PlayerGestureConfig = PlayerGestureConfig(),
    playbackBrightnessPercent: Int?,
    onPlaybackBrightnessChange: (Int) -> Unit,
    loadSubtitleCues: LoadSubtitleCues,
    captionStyles: CaptionStyles = CaptionStyles(),
    danmakuState: PlayerDanmakuState = PlayerDanmakuState(),
    onDanmakuAction: (PlayerDanmakuAction) -> Unit = {},
    hostTransitionProgress: () -> Float = { 0f },
) {
    val activity = LocalActivity.current
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val audioManager = remember(context) {
        context.getSystemService(AudioManager::class.java)
    }
    val gestureState = remember { PlayerGestureState() }
    var appliedBrightnessPercent by remember { mutableIntStateOf(-1) }
    var appliedVolumeLevel by remember { mutableIntStateOf(-1) }
    var controlsVisible by remember { mutableStateOf(true) }
    var optionsVisible by remember { mutableStateOf(false) }
    var chaptersVisible by remember { mutableStateOf(false) }
    var timelineScrubbing by remember { mutableStateOf(false) }
    val accessibleControls = rememberAccessiblePlayerControls(
        gestureConfig.accessibleControlsEnabled,
    )
    val audioOnlySnackbar = remember { SnackbarHostState() }
    val playbackStatus = rememberPlayerPlaybackStatus(
        player,
        onRetryPlayback.takeIf { stream.playbackContract == StreamPlaybackContract.ServerSabr },
    )
    val isInPip by rememberIsInPipMode()
    val isPipAvailable = remember(activity) { supportsPictureInPicture(activity) }
    val externalSubtitle = stream.subtitles.firstOrNull {
        stream.playbackContract == StreamPlaybackContract.ServerSabr &&
            it.key == selectedSubtitleKey
    }
    PlayerGestureInitializationEffect(
        state = gestureState,
        audioManager = audioManager,
        selectedBrightnessPercent = playbackBrightnessPercent,
        windowBrightness = activity?.window?.attributes?.screenBrightness,
        onVolumeInitialized = { appliedVolumeLevel = it },
    )

    PlaybackBrightnessEffect(
        window = activity?.window,
        isFullscreen = isFullscreen,
        selectedPercent = playbackBrightnessPercent,
    )
    PlaybackKeepScreenOnEffect(
        window = activity?.window,
        videoIsPlaying = playbackStatus.isPlaying && !audioOnlyState.active,
    )
    LaunchedEffect(accessibleControls) {
        if (accessibleControls) controlsVisible = true
    }
    val isSeekDragging = gestureState.dragMode.value == DragMode.Seek
    LaunchedEffect(
        controlsVisible,
        playbackStatus.isPlaying,
        accessibleControls,
        isSeekDragging,
        timelineScrubbing,
    ) {
        if (controlsVisible && playbackStatus.isPlaying && !accessibleControls) {
            if (isSeekDragging || timelineScrubbing) return@LaunchedEffect
            delay(AUTO_HIDE_DELAY_MS)
            controlsVisible = false
        }
    }

    PlayerAudioOnlyFailureEffect(audioOnlyState, audioOnlySnackbar)

    val presentationState = rememberPresentationState(player, keepContentOnReset = true)
    val surfaceKey = rememberPlayerSurfaceKey(stream.id)
    val chromeModifier = Modifier.graphicsLayer {
        alpha = 1f - hostTransitionProgress().coerceIn(0f, 1f)
    }
    val gesturesVisible by remember(hostTransitionProgress) {
        derivedStateOf { hostTransitionProgress() < 0.01f }
    }
    val controlsAllowedByProgress by remember(hostTransitionProgress) {
        derivedStateOf { hostTransitionProgress() < 0.99f }
    }
    PlayerFullscreenExitGestureBox(
        enabled = isFullscreen && playbackStatus.acceptsInput && !isInPip,
        onGestureFeedback = {
            controlsVisible = false
            hapticFeedback.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
        },
        onExitFullscreen = onToggleFullscreen,
        modifier = modifier,
    ) {
        if (audioOnlyState.active) {
            DeArrowAudioOnlyPoster(
                stream = stream,
                isPlaying = playbackStatus.isPlaying,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            ResilientPlayerSurface(
                player = player,
                surfaceKey = surfaceKey,
                resizeMode = gestureState.resizeMode.value,
                showNativeSubtitles = isInPip && externalSubtitle == null &&
                    selectedSubtitleKey != null,
                captionStyles = captionStyles,
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (!audioOnlyState.active && (!isInPip || externalSubtitle != null)) {
            Box(modifier = chromeModifier.fillMaxSize()) {
                DanmakuOverlay(
                    player = player,
                    state = danmakuState,
                    visible = !isInPip,
                    modifier = Modifier.fillMaxSize(),
                )
                PlayerSubtitleOverlay(
                    player = player,
                    controlsVisible = controlsVisible && !isInPip,
                    subtitlesVisible = selectedSubtitleKey != null,
                    externalSource = externalSubtitle,
                    loadExternalCues = loadSubtitleCues,
                    captionStyles = captionStyles,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        if (presentationState.coverSurface && !audioOnlyState.active) {
            PlayerLoadingPoster(stream = stream, modifier = Modifier.fillMaxSize())
        }

        PlayerBufferingIndicator(
            visible = playbackStatus.isBuffering && !presentationState.coverSurface && !isInPip,
            modifier = chromeModifier.align(Alignment.Center),
        )

        if (!isInPip && playbackStatus.acceptsInput && !accessibleControls &&
            gesturesVisible
        ) {
            PlayerGestureLayer(
                player = player,
                state = gestureState,
                onSingleTap = {
                    controlsVisible = !controlsVisible
                },
                onAdjustBrightness = { fraction ->
                    val percent = (fraction * 100).toInt()
                    if (percent != appliedBrightnessPercent) activity?.window?.let { window ->
                        appliedBrightnessPercent = percent
                        onPlaybackBrightnessChange(percent)
                        window.applyPlaybackBrightness(percent)
                    }
                },
                onAdjustVolume = { fraction ->
                    audioManager?.let { manager ->
                        val maxVolume = manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                        val target = (fraction * maxVolume).toInt().coerceIn(0, maxVolume)
                        if (target != appliedVolumeLevel) {
                            appliedVolumeLevel = target
                            manager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
                        }
                    }
                },
                onBrightnessGestureStart = {
                    val fraction = activity?.window?.attributes?.screenBrightness
                        ?.takeIf { it in 0f..1f }
                        ?: appliedBrightnessPercent
                            .takeIf { it in 0..100 }
                            ?.div(100f)
                        ?: gestureState.brightnessFraction.floatValue
                    gestureState.brightnessFraction.floatValue = fraction
                    fraction
                },
                onVolumeGestureStart = {
                    val fraction = audioManager?.let { manager ->
                        val maxVolume = manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                        if (maxVolume > 0) {
                            manager.getStreamVolume(AudioManager.STREAM_MUSIC) / maxVolume.toFloat()
                        } else {
                            0f
                        }
                    } ?: gestureState.volumeFraction.floatValue
                    gestureState.volumeFraction.floatValue = fraction
                    fraction
                },
                onGestureFeedback = {
                    controlsVisible = false
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                },
                isFullscreen = isFullscreen,
                onEnterFullscreenGesture = {
                    if (!isFullscreen) onToggleFullscreen()
                },
                onExitFullscreenGesture = {
                    if (isFullscreen) onToggleFullscreen()
                },
                fullscreenExitGestureEnabled = false,
                config = gestureConfig,
                modifier = Modifier.fillMaxSize(),
            )
        }

        AnimatedVisibility(
            visible = gestureState.seekDragOverlayActive.value,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            PlayerSeekScrubOverlay(
                player = player,
                positionMs = gestureState.seekDragTargetMs.longValue,
                segments = sponsorBlockPolicy.visibleSegments,
                isFullscreen = isFullscreen,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isFullscreen) {
                            Modifier.padding(start = 12.dp, end = 8.dp, bottom = 6.dp)
                        } else {
                            Modifier
                                .windowInsetsPadding(WindowInsets.navigationBars)
                                .padding(start = 4.dp, end = 4.dp)
                        },
                    ),
            )
        }

        AnimatedVisibility(
            visible = controlsAllowedByProgress &&
                (controlsVisible || accessibleControls) &&
                !isInPip && playbackStatus.acceptsInput,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            PlayerControls(
                player = player,
                title = stream.title,
                onNavigateBack = onNavigateBack,
                onOpenOptions = { optionsVisible = true },
                onOpenChapters = { chaptersVisible = true },
                onEnterPip = {
                    controlsVisible = false
                    enterPictureInPicture(
                        activity,
                        isPlaying = playbackStatus.isPlaying,
                        audioOnlyAvailable = audioOnlyState.available,
                        sourceRect = pipSourceRect,
                    )
                },
                onToggleFullscreen = onToggleFullscreen,
                onCycleResizeMode = {
                    gestureState.resizeMode.value = gestureState.resizeMode.value.next()
                },
                resizeMode = gestureState.resizeMode.value,
                isFullscreen = isFullscreen,
                isPipAvailable = isPipAvailable,
                chaptersAvailable = chapters.isNotEmpty(),
                sponsorBlockSegments = sponsorBlockPolicy.visibleSegments,
                seekPreviewPositionMs = gestureState.seekDragTargetMs.longValue
                    .takeIf { gestureState.seekDragOverlayActive.value },
                onTimelineScrubbingChange = { timelineScrubbing = it },
                modifier = chromeModifier.fillMaxSize(),
            )
        }

        PlayerSurfaceFeedback(
            player = player,
            isPlaying = playbackStatus.isPlaying,
            isInPip = isInPip,
            sponsorBlockPolicy = sponsorBlockPolicy,
        )

        PlayerSurfaceSheets(
            player = player,
            stream = stream,
            codecSupport = codecSupport,
            optionsVisible = optionsVisible,
            chaptersVisible = chaptersVisible,
            isInPip = isInPip,
            chapters = chapters,
            options = PlayerOptionsState(
                selectedCodec,
                selectedQuality,
                selectedAudioKey,
                selectedSubtitleKey,
                selectedSpeed,
                gestureState.resizeMode.value,
                audioOnlyState.active,
                audioOnlyState.changing,
                audioOnlyState.available,
                danmakuState,
            ),
            actions = PlayerOptionsActions(
                onSelectCodec,
                onSelectQuality,
                onSelectAudio,
                onSelectSubtitle,
                onSelectSpeed,
                { gestureState.resizeMode.value = it },
                audioOnlyState::setEnabled,
                onDanmakuAction,
            ),
            onDismissOptions = { optionsVisible = false },
            onDismissChapters = { chaptersVisible = false },
        )

        playbackStatus.error?.takeIf { !isInPip }?.let { error ->
            PlaybackFailureOverlay(
                error = error,
                onRetry = onRetryPlayback,
                onOpenAccounts = onOpenAccounts,
                onBack = onNavigateBack,
            )
        }

        PlayerSurfaceSnackbar(audioOnlySnackbar)
    }
}
