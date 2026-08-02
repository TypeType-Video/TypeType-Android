package dev.typetype.android.feature.player.components

import android.media.AudioManager
import android.graphics.Rect
import androidx.activity.compose.LocalActivity
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.ui.compose.state.rememberPresentationState
import dev.typetype.android.R
import dev.typetype.android.domain.stream.Chapter
import dev.typetype.android.domain.stream.SponsorBlockSegment
import dev.typetype.android.domain.stream.Stream
import dev.typetype.android.domain.stream.StreamPlaybackContract
import dev.typetype.android.feature.player.LoadSubtitleCues
import dev.typetype.android.feature.player.PlaybackCodecSupport
import dev.typetype.android.feature.player.key
import dev.typetype.android.feature.player.state.PlayerGestureState
import dev.typetype.android.feature.player.state.ResizeMode
import dev.typetype.android.feature.player.state.next
import kotlinx.coroutines.delay

private const val AUTO_HIDE_DELAY_MS = 3_500L

@OptIn(markerClass = [UnstableApi::class])
@Composable
internal fun PlayerSurfaceBox(
    player: MediaController,
    stream: Stream,
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
    sponsorBlockSegments: List<SponsorBlockSegment> = emptyList(),
    chapters: List<Chapter> = emptyList(),
    gestureConfig: PlayerGestureConfig = PlayerGestureConfig(),
    playbackBrightnessPercent: Int?,
    onPlaybackBrightnessChange: (Int) -> Unit,
    loadSubtitleCues: LoadSubtitleCues,
) {
    val activity = LocalActivity.current
    val context = LocalContext.current
    val audioManager = remember(context) {
        context.getSystemService(AudioManager::class.java)
    }
    val gestureState = remember { PlayerGestureState() }
    var appliedBrightnessPercent by remember { mutableIntStateOf(-1) }
    var appliedVolumeLevel by remember { mutableIntStateOf(-1) }
    var controlsVisible by remember { mutableStateOf(true) }
    var optionsVisible by remember { mutableStateOf(false) }
    var chaptersVisible by remember { mutableStateOf(false) }
    val audioOnlyState = rememberAudioOnlyPlaybackState(player, stream)
    val audioOnlySnackbar = remember { SnackbarHostState() }
    val audioOnlyUnavailable = stringResource(R.string.player_audio_only_unavailable)
    val audioOnlyNetworkFailure = stringResource(R.string.error_network_unavailable)
    val playbackStatus = rememberPlayerPlaybackStatus(player)
    val isInPip by rememberIsInPipMode()
    val isPipAvailable = remember(activity) { supportsPictureInPicture(activity) }
    val externalSubtitle = stream.subtitles.firstOrNull {
        stream.playbackContract == StreamPlaybackContract.ServerSabr &&
            it.key == selectedSubtitleKey
    }

    LaunchedEffect(Unit) {
        gestureState.brightnessFraction.floatValue = playbackBrightnessPercent
            ?.let { it / 100f }
            ?: activity?.window?.attributes?.screenBrightness
                ?.takeIf { it in 0f..1f }
            ?: 0.5f
        val maxVolume = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 1
        val currentVolume = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
        gestureState.volumeFraction.floatValue =
            if (maxVolume > 0) currentVolume / maxVolume.toFloat() else 0f
        appliedVolumeLevel = currentVolume
    }

    PlaybackBrightnessEffect(
        window = activity?.window,
        isFullscreen = isFullscreen,
        selectedPercent = playbackBrightnessPercent,
    )
    PlaybackKeepScreenOnEffect(
        window = activity?.window,
        videoIsPlaying = playbackStatus.isPlaying && !audioOnlyState.active,
    )

    LaunchedEffect(controlsVisible, playbackStatus.isPlaying) {
        if (controlsVisible && playbackStatus.isPlaying) {
            delay(AUTO_HIDE_DELAY_MS)
            controlsVisible = false
        }
    }

    LaunchedEffect(audioOnlyState.failure) {
        val failure = audioOnlyState.failure ?: return@LaunchedEffect
        audioOnlySnackbar.showSnackbar(
            when (failure) {
                AudioOnlyPlaybackFailure.Network -> audioOnlyNetworkFailure
                AudioOnlyPlaybackFailure.Unavailable -> audioOnlyUnavailable
            },
        )
        audioOnlyState.consumeFailure()
    }

    val presentationState = rememberPresentationState(player)
    val surfaceKey = rememberPlayerSurfaceKey(stream.id)
    Box(modifier = modifier.background(Color.Black).clipToBounds()) {
        if (audioOnlyState.active) {
            AudioOnlyPoster(
                thumbnailUrl = stream.thumbnailUrl,
                title = stream.title,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            ResilientPlayerSurface(
                player = player,
                surfaceKey = surfaceKey,
                resizeMode = gestureState.resizeMode.value,
                showNativeSubtitles = isInPip && selectedSubtitleKey != null,
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (!audioOnlyState.active && (!isInPip || externalSubtitle != null)) {
            PlayerSubtitleOverlay(
                player = player,
                controlsVisible = controlsVisible && !isInPip,
                subtitlesVisible = selectedSubtitleKey != null,
                externalSource = externalSubtitle,
                loadExternalCues = loadSubtitleCues,
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (presentationState.coverSurface && !audioOnlyState.active) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }

        AnimatedVisibility(
            visible = playbackStatus.isBuffering && !presentationState.coverSurface && !isInPip,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center),
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }

        if (!isInPip && playbackStatus.acceptsInput) {
            PlayerGestureLayer(
                player = player,
                state = gestureState,
                onTogglePlayPause = {
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
                onGestureFeedback = { controlsVisible = false },
                isFullscreen = isFullscreen,
                onEnterFullscreenGesture = {
                    if (!isFullscreen) onToggleFullscreen()
                },
                onExitFullscreenGesture = {
                    if (isFullscreen) onToggleFullscreen() else onNavigateBack()
                },
                config = gestureConfig,
                modifier = Modifier.fillMaxSize(),
            )
        }

        AnimatedVisibility(
            visible = controlsVisible && !isInPip && playbackStatus.acceptsInput,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            PlayerControls(
                player = player,
                onNavigateBack = onNavigateBack,
                onOpenOptions = { optionsVisible = true },
                onOpenChapters = { chaptersVisible = true },
                onEnterPip = {
                    controlsVisible = false
                    enterPictureInPicture(
                        activity,
                        isPlaying = playbackStatus.isPlaying,
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
                sponsorBlockSegments = sponsorBlockSegments,
                modifier = Modifier.fillMaxSize(),
            )
        }

        PipPlaybackStateOverlay(
            visible = isInPip,
            isPlaying = playbackStatus.isPlaying,
            modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
        )

        SponsorBlockSkipper(player = player, segments = sponsorBlockSegments)

        if (optionsVisible && !isInPip) {
            PlaybackOptionsSheet(
                player = player,
                stream = stream,
                selectedCodec = selectedCodec,
                selectedQuality = selectedQuality,
                selectedAudioKey = selectedAudioKey,
                selectedSubtitleKey = selectedSubtitleKey,
                selectedSpeed = selectedSpeed,
                codecSupport = codecSupport,
                resizeMode = gestureState.resizeMode.value,
                audioOnlyEnabled = audioOnlyState.active,
                audioOnlyChanging = audioOnlyState.changing,
                showAudioOnly = audioOnlyState.available,
                onSelectCodec = onSelectCodec,
                onSelectQuality = onSelectQuality,
                onSelectAudio = onSelectAudio,
                onSelectSubtitle = onSelectSubtitle,
                onSelectSpeed = onSelectSpeed,
                onSelectResizeMode = { gestureState.resizeMode.value = it },
                onAudioOnlyChange = audioOnlyState::setEnabled,
                onDismiss = { optionsVisible = false },
            )
        }

        if (chaptersVisible && chapters.isNotEmpty() && !isInPip) {
            ChaptersSheet(
                chapters = chapters,
                currentPositionMs = player.currentPosition,
                onChapterClick = { chapter ->
                    player.seekTo(chapter.startMs)
                    chaptersVisible = false
                },
                onDismiss = { chaptersVisible = false },
            )
        }

        playbackStatus.error?.takeIf { !isInPip }?.let { error ->
            PlaybackFailureOverlay(
                error = error,
                onRetry = onRetryPlayback,
                onOpenAccounts = onOpenAccounts,
                onBack = onNavigateBack,
            )
        }

        SnackbarHost(
            hostState = audioOnlySnackbar,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
        )
    }
}
