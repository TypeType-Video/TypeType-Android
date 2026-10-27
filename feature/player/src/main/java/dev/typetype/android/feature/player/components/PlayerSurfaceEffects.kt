package dev.typetype.android.feature.player.components

import android.media.AudioManager
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import dev.typetype.android.R
import dev.typetype.android.feature.player.state.PlayerGestureState

@Composable
internal fun PlayerGestureInitializationEffect(
    state: PlayerGestureState,
    audioManager: AudioManager?,
    selectedBrightnessPercent: Int?,
    windowBrightness: Float?,
    onVolumeInitialized: (Int) -> Unit,
) {
    LaunchedEffect(Unit) {
        state.brightnessFraction.floatValue = selectedBrightnessPercent
            ?.let { it / 100f }
            ?: windowBrightness?.takeIf { it in 0f..1f }
            ?: 0.5f
        val maxVolume = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 1
        val currentVolume = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
        state.volumeFraction.floatValue =
            if (maxVolume > 0) currentVolume / maxVolume.toFloat() else 0f
        onVolumeInitialized(currentVolume)
    }
}

@Composable
internal fun PlayerAudioOnlyFailureEffect(
    state: AudioOnlyPlaybackState,
    snackbar: SnackbarHostState,
) {
    val unavailable = stringResource(R.string.player_audio_only_unavailable)
    val networkFailure = stringResource(R.string.error_network_unavailable)
    LaunchedEffect(state.failure) {
        val failure = state.failure ?: return@LaunchedEffect
        snackbar.showSnackbar(
            when (failure) {
                AudioOnlyPlaybackFailure.Network -> networkFailure
                AudioOnlyPlaybackFailure.Unavailable -> unavailable
            },
        )
        state.consumeFailure()
    }
}
