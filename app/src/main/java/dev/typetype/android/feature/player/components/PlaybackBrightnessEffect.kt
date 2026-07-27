package dev.typetype.android.feature.player.components

import android.view.Window
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect

@Composable
internal fun PlaybackBrightnessEffect(
    window: Window?,
    isFullscreen: Boolean,
    selectedPercent: Int?,
) {
    LaunchedEffect(window, isFullscreen, selectedPercent) {
        if (isFullscreen && selectedPercent != null) {
            window?.applyPlaybackBrightness(selectedPercent)
        } else {
            window?.clearPlaybackBrightnessOverride()
        }
    }
    DisposableEffect(window) {
        onDispose {
            window?.clearPlaybackBrightnessOverride()
        }
    }
}

internal fun Window.applyPlaybackBrightness(percent: Int) {
    val target = percent.coerceIn(0, 100) / 100f
    val params = attributes
    if (params.screenBrightness == target) return
    params.screenBrightness = target
    attributes = params
}

internal fun Window.clearPlaybackBrightnessOverride() {
    val params = attributes
    if (params.screenBrightness == WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE) return
    params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
    attributes = params
}
