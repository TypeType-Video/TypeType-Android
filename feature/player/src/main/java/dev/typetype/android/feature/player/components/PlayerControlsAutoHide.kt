package dev.typetype.android.feature.player.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay

@Composable
internal fun PlayerControlsAutoHideEffect(
    controlsVisible: Boolean,
    isPlaying: Boolean,
    accessibleControls: Boolean,
    holding: Boolean,
    onHide: () -> Unit,
) {
    LaunchedEffect(controlsVisible, isPlaying, accessibleControls, holding) {
        if (controlsVisible && isPlaying && !accessibleControls && !holding) {
            delay(AUTO_HIDE_DELAY_MS)
            onHide()
        }
    }
}

private const val AUTO_HIDE_DELAY_MS = 3_500L
