package dev.typetype.android.feature.player

import dev.typetype.android.feature.player.state.ResizeMode

internal data class EndScreenActions(
    val onOpenVideo: (videoUrl: String) -> Unit,
    val onOpenChannel: (channelUrl: String) -> Unit,
    val onOpenPlaylist: (playlistUrl: String) -> Unit,
)

internal data class EndScreenFrame(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
)

internal fun endScreenFrame(
    containerWidth: Float,
    containerHeight: Float,
    videoAspectRatio: Float,
    resizeMode: ResizeMode,
): EndScreenFrame {
    val container = EndScreenFrame(0f, 0f, containerWidth, containerHeight)
    if (containerWidth <= 0f || containerHeight <= 0f || videoAspectRatio <= 0f) return container
    val widthAtFullHeight = containerHeight * videoAspectRatio
    val width = when (resizeMode) {
        ResizeMode.Stretch -> return container
        ResizeMode.Fit -> minOf(containerWidth, widthAtFullHeight)
        ResizeMode.Crop -> maxOf(containerWidth, widthAtFullHeight)
    }
    val height = width / videoAspectRatio
    return EndScreenFrame(
        left = (containerWidth - width) / 2f,
        top = (containerHeight - height) / 2f,
        width = width,
        height = height,
    )
}
