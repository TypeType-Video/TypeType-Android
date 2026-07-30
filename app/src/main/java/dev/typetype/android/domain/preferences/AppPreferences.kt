package dev.typetype.android.domain.preferences

data class AppPreferences(
    val accentColor: AccentColor = AccentColor.Blue,
    val playerDoubleTapSeekEnabled: Boolean = true,
    val playerSwipeSeekEnabled: Boolean = true,
    val playerSwipeBrightnessVolumeEnabled: Boolean = true,
    val playerLongPressSpeedEnabled: Boolean = true,
    val playerAutoplayEnabled: Boolean = true,
    val playerAutoplayCountdownSeconds: Int = 10,
    val playerPauseInBackground: Boolean = false,
)
