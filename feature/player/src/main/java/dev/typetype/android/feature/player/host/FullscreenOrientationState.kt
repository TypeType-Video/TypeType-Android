package dev.typetype.android.feature.player.host

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver

internal enum class DeviceOrientation { Portrait, Landscape, Other }

// The device class, not the space left by the system bars: on a large foldable that space crossed
// 600 dp each time fullscreen hid the bars, and the player looped in and out of fullscreen.
internal fun allowsRotationFullscreen(smallestScreenWidthDp: Int): Boolean =
    smallestScreenWidthDp < TABLET_SMALLEST_WIDTH_DP

private const val TABLET_SMALLEST_WIDTH_DP = 600

internal data class FullscreenOrientationState(
    val locksLandscape: Boolean = false,
    val suppressesLandscapeEntry: Boolean = false,
    val restoresPortraitOnExit: Boolean = false,
) {
    fun onUserRequest(
        fullscreen: Boolean,
        orientation: DeviceOrientation,
    ): FullscreenOrientationTransition = if (fullscreen) {
        FullscreenOrientationTransition(
            state = copy(
                locksLandscape = true,
                suppressesLandscapeEntry = false,
                restoresPortraitOnExit = orientation == DeviceOrientation.Portrait,
            ),
            fullscreenRequest = true,
        )
    } else {
        FullscreenOrientationTransition(
            state = copy(
                locksLandscape = false,
                suppressesLandscapeEntry = orientation == DeviceOrientation.Landscape,
                restoresPortraitOnExit = locksLandscape,
            ),
            fullscreenRequest = false,
        )
    }

    fun onEnvironmentChanged(
        orientation: DeviceOrientation,
        hasFullscreenMedia: Boolean,
        allowsRotationFullscreen: Boolean,
        isFullscreen: Boolean,
    ): FullscreenOrientationTransition {
        if (!hasFullscreenMedia) {
            return FullscreenOrientationTransition(
                state = FullscreenOrientationState(),
                fullscreenRequest = false.takeIf { isFullscreen },
            )
        }
        if (!allowsRotationFullscreen) {
            return FullscreenOrientationTransition(
                state = copy(locksLandscape = locksLandscape && isFullscreen),
                fullscreenRequest = false.takeIf { isFullscreen && !locksLandscape },
            )
        }

        return when (orientation) {
            DeviceOrientation.Portrait -> FullscreenOrientationTransition(
                state = copy(
                    locksLandscape = locksLandscape && isFullscreen,
                    suppressesLandscapeEntry = false,
                    restoresPortraitOnExit = false,
                ),
                fullscreenRequest = false.takeIf { isFullscreen && !locksLandscape },
            )

            DeviceOrientation.Landscape -> FullscreenOrientationTransition(
                state = copy(locksLandscape = locksLandscape && isFullscreen),
                fullscreenRequest = true.takeIf {
                    !isFullscreen && !suppressesLandscapeEntry
                },
            )

            DeviceOrientation.Other -> FullscreenOrientationTransition(
                state = copy(locksLandscape = locksLandscape && isFullscreen),
            )
        }
    }

    companion object {
        val Saver: Saver<FullscreenOrientationState, Any> = listSaver(
            save = {
                listOf(it.locksLandscape, it.suppressesLandscapeEntry, it.restoresPortraitOnExit)
            },
            restore = {
                FullscreenOrientationState(
                    locksLandscape = it[0],
                    suppressesLandscapeEntry = it[1],
                    restoresPortraitOnExit = it.getOrNull(2) == true,
                )
            },
        )
    }
}

internal data class FullscreenOrientationTransition(
    val state: FullscreenOrientationState,
    val fullscreenRequest: Boolean? = null,
)
