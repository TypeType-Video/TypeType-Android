package dev.typetype.android.core.ui.util

import android.os.Build
import android.view.Window
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Toggles edge-to-edge fullscreen on a window. Mirrors LibreTube's WindowHelper
 * (see /home/xut/Documents/Source/LibreTube/.../helpers/WindowHelper.kt:17-42)
 * and PipePipe's showSystemUi/hideSystemUi pair
 * (see VideoDetailFragment.java:2233-2286).
 *
 * Use SHORT_EDGES only in fullscreen so video can extend behind the cutout in
 * landscape; in non-fullscreen rely on Android 9+'s default "content under
 * cutout in portrait" semantics, which avoids the dim status-bar scrim that
 * SHORT_EDGES otherwise leaves on top of dark content.
 */
object WindowHelper {

    fun toggleFullscreen(window: Window, isFullscreen: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode = if (isFullscreen) {
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                } else {
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
                }
            }
        }

        WindowCompat.setDecorFitsSystemWindows(window, !isFullscreen)

        val noLimits = WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        if (isFullscreen) {
            window.setFlags(noLimits, noLimits)
        } else {
            window.clearFlags(noLimits)
        }

        val controller = WindowCompat.getInsetsController(window, window.decorView)
        if (isFullscreen) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}
