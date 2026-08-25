package dev.typetype.android.feature.player

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import dev.typetype.android.core.ui.util.WindowHelper

@Composable
internal fun PlayerFullscreenEffect(
    activity: Activity?,
    isFullscreen: Boolean,
    locksLandscape: Boolean,
    restoresPortraitOnExit: Boolean = false,
) {
    LaunchedEffect(isFullscreen, locksLandscape, restoresPortraitOnExit) {
        val window = activity?.window ?: return@LaunchedEffect
        if (isFullscreen) {
            activity.requestedOrientation = if (locksLandscape) {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            } else {
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
            WindowHelper.toggleFullscreen(window, isFullscreen = true)
        } else {
            activity.requestedOrientation = if (restoresPortraitOnExit) {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            } else {
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
            WindowHelper.toggleFullscreen(window, isFullscreen = false)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            val window = activity?.window ?: return@onDispose
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            WindowHelper.toggleFullscreen(window, isFullscreen = false)
        }
    }
}
