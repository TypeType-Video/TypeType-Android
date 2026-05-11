package dev.typetype.android.feature.player.components

import androidx.compose.ui.unit.Dp
import dev.typetype.android.feature.player.state.ResizeMode

data class SurfaceSize(
    val width: Dp,
    val height: Dp,
)

fun targetVideoSurfaceSize(
    resizeMode: ResizeMode,
    containerWidth: Dp,
    containerHeight: Dp,
    containerAspect: Float,
    videoAspect: Float,
): SurfaceSize = when (resizeMode) {
    ResizeMode.Fit -> if (videoAspect > containerAspect) {
        SurfaceSize(containerWidth, containerWidth / videoAspect)
    } else {
        SurfaceSize(containerHeight * videoAspect, containerHeight)
    }
    ResizeMode.Crop -> if (videoAspect > containerAspect) {
        SurfaceSize(containerHeight * videoAspect, containerHeight)
    } else {
        SurfaceSize(containerWidth, containerWidth / videoAspect)
    }
    ResizeMode.Stretch -> SurfaceSize(containerWidth, containerHeight)
}
