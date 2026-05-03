package dev.typetype.android.feature.settings.player

sealed interface PlayerSettingsAction {
    data class SetDoubleTapSeek(val enabled: Boolean) : PlayerSettingsAction
    data class SetSwipeSeek(val enabled: Boolean) : PlayerSettingsAction
    data class SetSwipeBrightnessVolume(val enabled: Boolean) : PlayerSettingsAction
    data class SetLongPressSpeed(val enabled: Boolean) : PlayerSettingsAction
    data class SetAutoplay(val enabled: Boolean) : PlayerSettingsAction
    data class SetPauseInBackground(val enabled: Boolean) : PlayerSettingsAction
}
