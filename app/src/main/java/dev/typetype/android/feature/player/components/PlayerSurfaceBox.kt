package dev.typetype.android.feature.player.components

import android.media.AudioManager
import androidx.activity.compose.LocalActivity
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.state.rememberPresentationState
import dev.typetype.android.domain.stream.Chapter
import dev.typetype.android.domain.stream.SponsorBlockSegment
import dev.typetype.android.domain.stream.Stream
import dev.typetype.android.feature.player.state.PlayerGestureState
import dev.typetype.android.feature.player.state.ResizeMode
import dev.typetype.android.feature.player.state.next
import kotlinx.coroutines.delay

private const val AUTO_HIDE_DELAY_MS = 3_500L

@OptIn(markerClass = [UnstableApi::class])
@Composable
fun PlayerSurfaceBox(
    player: Player,
    stream: Stream,
    selectedQuality: String,
    selectedAudioKey: String?,
    selectedSubtitleKey: String?,
    selectedSpeed: Float,
    onSelectQuality: (String) -> Unit,
    onSelectAudio: (String?) -> Unit,
    onSelectSubtitle: (String?) -> Unit,
    onSelectSpeed: (Float) -> Unit,
    onNavigateBack: () -> Unit,
    isFullscreen: Boolean = false,
    onToggleFullscreen: () -> Unit = {},
    sponsorBlockSegments: List<SponsorBlockSegment> = emptyList(),
    chapters: List<Chapter> = emptyList(),
    gestureConfig: PlayerGestureConfig = PlayerGestureConfig(),
    modifier: Modifier = Modifier,
) {
    val activity = LocalActivity.current
    val context = LocalContext.current
    val audioManager = remember(context) {
        context.getSystemService(AudioManager::class.java)
    }
    val gestureState = remember { PlayerGestureState() }
    var controlsVisible by remember { mutableStateOf(true) }
    var optionsVisible by remember { mutableStateOf(false) }
    var chaptersVisible by remember { mutableStateOf(false) }
    val playbackStatus = rememberPlayerPlaybackStatus(player)
    val isInPip by rememberIsInPipMode()

    LaunchedEffect(Unit) {
        gestureState.brightnessFraction.floatValue = activity?.window?.attributes?.screenBrightness
            ?.takeIf { it in 0f..1f } ?: 0.5f
        val maxVolume = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 1
        val currentVolume = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
        gestureState.volumeFraction.floatValue =
            if (maxVolume > 0) currentVolume / maxVolume.toFloat() else 0f
    }

    LaunchedEffect(controlsVisible, playbackStatus.isPlaying) {
        if (controlsVisible && playbackStatus.isPlaying) {
            delay(AUTO_HIDE_DELAY_MS)
            controlsVisible = false
        }
    }

    val presentationState = rememberPresentationState(player)
    val surfaceKey = rememberPlayerSurfaceKey(stream.id)
    Box(modifier = modifier.background(Color.Black).clipToBounds()) {
        ResilientPlayerSurface(
            player = player,
            surfaceKey = surfaceKey,
            resizeMode = gestureState.resizeMode.value,
            modifier = Modifier.fillMaxSize(),
        )

        if (!isInPip) {
            PlayerSubtitleOverlay(
                player = player,
                controlsVisible = controlsVisible,
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (presentationState.coverSurface) {
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

        if (!isInPip) {
            PlayerGestureLayer(
                player = player,
                state = gestureState,
                onTogglePlayPause = {
                    controlsVisible = !controlsVisible
                },
                onAdjustBrightness = { fraction ->
                    activity?.window?.let { window ->
                        val params = window.attributes
                        params.screenBrightness = fraction
                        window.attributes = params
                    }
                },
                onAdjustVolume = { fraction ->
                    audioManager?.let { manager ->
                        val maxVolume = manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                        val target = (fraction * maxVolume).toInt().coerceIn(0, maxVolume)
                        manager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
                    }
                },
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
            visible = controlsVisible && !isInPip,
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
                    enterPictureInPicture(activity)
                },
                onToggleFullscreen = onToggleFullscreen,
                onCycleResizeMode = {
                    gestureState.resizeMode.value = gestureState.resizeMode.value.next()
                },
                resizeMode = gestureState.resizeMode.value,
                isFullscreen = isFullscreen,
                chaptersAvailable = chapters.isNotEmpty(),
                sponsorBlockSegments = sponsorBlockSegments,
                modifier = Modifier.fillMaxSize(),
            )
        }

        SponsorBlockSkipper(player = player, segments = sponsorBlockSegments)

        if (optionsVisible && !isInPip) {
            PlaybackOptionsSheet(
                player = player,
                stream = stream,
                selectedQuality = selectedQuality,
                selectedAudioKey = selectedAudioKey,
                selectedSubtitleKey = selectedSubtitleKey,
                selectedSpeed = selectedSpeed,
                resizeMode = gestureState.resizeMode.value,
                onSelectQuality = onSelectQuality,
                onSelectAudio = onSelectAudio,
                onSelectSubtitle = onSelectSubtitle,
                onSelectSpeed = onSelectSpeed,
                onSelectResizeMode = { gestureState.resizeMode.value = it },
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
    }
}
