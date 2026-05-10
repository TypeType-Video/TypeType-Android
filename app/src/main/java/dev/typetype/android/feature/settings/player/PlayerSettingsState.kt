package dev.typetype.android.feature.settings.player

data class PlayerSettingsState(
    val doubleTapSeekEnabled: Boolean = true,
    val swipeSeekEnabled: Boolean = true,
    val swipeBrightnessVolumeEnabled: Boolean = true,
    val longPressSpeedEnabled: Boolean = true,
    val autoplayEnabled: Boolean = true,
    val pauseInBackgroundEnabled: Boolean = false,
    val defaultQuality: String = "1080p",
    val defaultService: Int = 0,
    val subtitlesEnabled: Boolean = false,
    val defaultSubtitleLanguage: String = "",
    val defaultAudioLanguage: String = "",
    val preferOriginalLanguage: Boolean = false,
)
