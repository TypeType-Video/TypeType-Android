package dev.typetype.android.domain.preferences

data class AppPreferences(
    val accentColor: AccentColor = AccentColor.Blue,
    val playerDoubleTapSeekEnabled: Boolean = true,
    val playerDoubleTapSeekSeconds: Int = 10,
    val playerPreferredCodec: String = "recommended",
    val playerSwipeSeekEnabled: Boolean = true,
    val playerSwipeBrightnessVolumeEnabled: Boolean = true,
    val playerPlaybackBrightnessPercent: Int? = null,
    val playerLongPressSpeedEnabled: Boolean = true,
    val playerAutoplayEnabled: Boolean = true,
    val playerAutoplayCountdownSeconds: Int = 10,
    val playerAudioOnlyPlayback: Boolean = false,
    val playerPauseInBackground: Boolean = false,
    val danmakuEnabled: Boolean = false,
    val danmakuSpeed: Float = 1f,
    val danmakuSize: Float = 1f,
)
