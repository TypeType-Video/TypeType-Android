package dev.typetype.android.feature.settings.player

sealed interface PlayerSettingsAction {
    data class SetDoubleTapSeek(val enabled: Boolean) : PlayerSettingsAction
    data class SetSwipeSeek(val enabled: Boolean) : PlayerSettingsAction
    data class SetSwipeBrightnessVolume(val enabled: Boolean) : PlayerSettingsAction
    data class SetLongPressSpeed(val enabled: Boolean) : PlayerSettingsAction
    data class SetAutoplay(val enabled: Boolean) : PlayerSettingsAction
    data class SetPauseInBackground(val enabled: Boolean) : PlayerSettingsAction
    data class SetDefaultQuality(val quality: String) : PlayerSettingsAction
    data class SetDefaultService(val service: Int) : PlayerSettingsAction
    data class SetSubtitlesEnabled(val enabled: Boolean) : PlayerSettingsAction
    data class SetSubtitleLanguage(val language: String) : PlayerSettingsAction
    data class SetAudioLanguage(val language: String) : PlayerSettingsAction
    data class SetPreferOriginalLanguage(val enabled: Boolean) : PlayerSettingsAction
}
