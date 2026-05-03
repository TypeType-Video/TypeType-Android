package dev.typetype.android.feature.settings.player

data class PlayerSettingsState(
    val doubleTapSeekEnabled: Boolean = true,
    val swipeSeekEnabled: Boolean = true,
    val swipeBrightnessVolumeEnabled: Boolean = true,
    val longPressSpeedEnabled: Boolean = true,
    val autoplayEnabled: Boolean = true,
    val pauseInBackgroundEnabled: Boolean = false,
)
