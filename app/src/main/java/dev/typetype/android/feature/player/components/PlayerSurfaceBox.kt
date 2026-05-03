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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_SURFACE_VIEW
import androidx.media3.ui.compose.state.rememberPresentationState
import androidx.media3.ui.compose.state.rememberPlayPauseButtonState
import dev.typetype.android.domain.stream.SponsorBlockSegment
import dev.typetype.android.feature.player.state.PlayerGestureState
import kotlinx.coroutines.delay

private const val AUTO_HIDE_DELAY_MS = 3_500L

@OptIn(markerClass = [UnstableApi::class])
@Composable
fun PlayerSurfaceBox(
    player: Player,
    onNavigateBack: () -> Unit,
    sponsorBlockSegments: List<SponsorBlockSegment> = emptyList(),
    modifier: Modifier = Modifier,
) {
    val activity = LocalActivity.current
    val context = LocalContext.current
    val audioManager = remember(context) {
        context.getSystemService(AudioManager::class.java)
    }
    val gestureState = remember { PlayerGestureState() }
    val playPauseState = rememberPlayPauseButtonState(player)
    var controlsVisible by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        gestureState.brightnessFraction.floatValue = activity?.window?.attributes?.screenBrightness
            ?.takeIf { it in 0f..1f } ?: 0.5f
        val maxVolume = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 1
        val currentVolume = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
        gestureState.volumeFraction.floatValue =
            if (maxVolume > 0) currentVolume / maxVolume.toFloat() else 0f
    }

    LaunchedEffect(controlsVisible, player.isPlaying) {
        if (controlsVisible && player.isPlaying) {
            delay(AUTO_HIDE_DELAY_MS)
            controlsVisible = false
        }
    }

    Box(modifier = modifier.background(Color.Black)) {
        PlayerSurface(
            player = player,
            surfaceType = SURFACE_TYPE_SURFACE_VIEW,
            modifier = Modifier.fillMaxSize(),
        )

        val presentationState = rememberPresentationState(player)
        if (presentationState.coverSurface) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }

        PlayerGestureLayer(
            player = player,
            state = gestureState,
            onTogglePlayPause = {
                controlsVisible = !controlsVisible
                if (!controlsVisible && playPauseState.isEnabled) {
                    playPauseState.onClick()
                }
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
            modifier = Modifier.fillMaxSize(),
        )

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            PlayerControls(
                player = player,
                onNavigateBack = onNavigateBack,
                sponsorBlockSegments = sponsorBlockSegments,
                modifier = Modifier.fillMaxSize(),
            )
        }

        SponsorBlockSkipper(player = player, segments = sponsorBlockSegments)
    }
}
