package dev.typetype.android.feature.player.components

import android.app.Activity
import android.media.AudioManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.media3.session.MediaController
import dev.typetype.android.feature.player.state.PlayerGestureState

@Composable
internal fun PlayerSurfaceGestureLayer(
    player: MediaController,
    state: PlayerGestureState,
    activity: Activity?,
    audioManager: AudioManager?,
    hapticFeedback: HapticFeedback,
    playbackStatus: PlayerPlaybackStatus,
    isInPip: Boolean,
    isFullscreen: Boolean,
    accessibleControls: Boolean,
    gesturesVisible: Boolean,
    controlsVisible: Boolean,
    config: PlayerGestureConfig,
    appliedBrightnessPercent: Int,
    onAppliedBrightnessChange: (Int) -> Unit,
    onPlaybackBrightnessChange: (Int) -> Unit,
    appliedVolumeLevel: Int,
    onAppliedVolumeChange: (Int) -> Unit,
    onControlsVisibleChange: (Boolean) -> Unit,
    onToggleFullscreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!isInPip && playbackStatus.acceptsInput && !accessibleControls && gesturesVisible) {
        PlayerGestureLayer(
            player = player,
            state = state,
            onSingleTap = { onControlsVisibleChange(!controlsVisible) },
            onAdjustBrightness = { fraction ->
                val percent = (fraction * 100).toInt()
                if (percent != appliedBrightnessPercent) activity?.window?.let { window ->
                    onAppliedBrightnessChange(percent)
                    onPlaybackBrightnessChange(percent)
                    window.applyPlaybackBrightness(percent)
                }
            },
            onAdjustVolume = { fraction ->
                audioManager?.let { manager ->
                    val maxVolume = manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                    val target = (fraction * maxVolume).toInt().coerceIn(0, maxVolume)
                    if (target != appliedVolumeLevel) {
                        onAppliedVolumeChange(target)
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
                    ?: state.brightnessFraction.floatValue
                state.brightnessFraction.floatValue = fraction
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
                } ?: state.volumeFraction.floatValue
                state.volumeFraction.floatValue = fraction
                fraction
            },
            onGestureFeedback = {
                onControlsVisibleChange(false)
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
            config = config,
            modifier = modifier,
        )
    }
}
