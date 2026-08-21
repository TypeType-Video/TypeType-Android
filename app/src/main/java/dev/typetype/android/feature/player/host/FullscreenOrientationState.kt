package dev.typetype.android.feature.player.host

internal enum class DeviceOrientation { Portrait, Landscape, Other }

internal data class FullscreenOrientationState(
    val locksLandscape: Boolean = false,
    val suppressesLandscapeEntry: Boolean = false,
) {
    fun onUserRequest(
        fullscreen: Boolean,
        orientation: DeviceOrientation,
    ): FullscreenOrientationTransition = if (fullscreen) {
        FullscreenOrientationTransition(
            state = copy(
                locksLandscape = true,
                suppressesLandscapeEntry = false,
            ),
            fullscreenRequest = true,
        )
    } else {
        FullscreenOrientationTransition(
            state = copy(
                locksLandscape = false,
                suppressesLandscapeEntry = orientation == DeviceOrientation.Landscape,
            ),
            fullscreenRequest = false,
        )
    }

    fun onEnvironmentChanged(
        orientation: DeviceOrientation,
        allowsRotationFullscreen: Boolean,
        isFullscreen: Boolean,
    ): FullscreenOrientationTransition {
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
}

internal data class FullscreenOrientationTransition(
    val state: FullscreenOrientationState,
    val fullscreenRequest: Boolean? = null,
)
