package dev.typetype.android.feature.player.components

internal enum class PlayerDoubleTapAction {
    Rewind,
    TogglePlayback,
    Forward,
}

internal fun doubleTapAction(x: Float, width: Float): PlayerDoubleTapAction {
    if (width <= 0f) return PlayerDoubleTapAction.TogglePlayback
    return when {
        x < width / 3f -> PlayerDoubleTapAction.Rewind
        x > width * 2f / 3f -> PlayerDoubleTapAction.Forward
        else -> PlayerDoubleTapAction.TogglePlayback
    }
}

internal fun PlayerDoubleTapAction.isEnabled(seekEnabled: Boolean): Boolean =
    this == PlayerDoubleTapAction.TogglePlayback || seekEnabled
