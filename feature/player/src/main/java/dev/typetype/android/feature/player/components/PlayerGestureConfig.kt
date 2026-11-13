package dev.typetype.android.feature.player.components

data class PlayerGestureConfig(
    val doubleTapSeekEnabled: Boolean = true,
    val doubleTapSeekSeconds: Int = 10,
    val swipeSeekEnabled: Boolean = false,
    val swipeBrightnessVolumeEnabled: Boolean = true,
    val longPressSpeedEnabled: Boolean = true,
    val longPressSpeed: Float = DEFAULT_LONG_PRESS_SPEED,
    val accessibleControlsEnabled: Boolean = false,
)

const val DEFAULT_LONG_PRESS_SPEED = 2f
