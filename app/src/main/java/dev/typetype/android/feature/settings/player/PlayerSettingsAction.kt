package dev.typetype.android.feature.settings.player

import dev.typetype.android.domain.usersettings.SponsorBlockMode

sealed interface PlayerSettingsAction {
    data class SetDoubleTapSeek(val enabled: Boolean) : PlayerSettingsAction
    data class SetSwipeSeek(val enabled: Boolean) : PlayerSettingsAction
    data class SetSwipeBrightnessVolume(val enabled: Boolean) : PlayerSettingsAction
    data class SetLongPressSpeed(val enabled: Boolean) : PlayerSettingsAction
    data class SetAutoplay(val enabled: Boolean) : PlayerSettingsAction
    data class SetAutoplayCountdown(val seconds: Int) : PlayerSettingsAction
    data class SetSkipPlaylistAutoplayScreen(val enabled: Boolean) : PlayerSettingsAction
    data class SetPauseInBackground(val enabled: Boolean) : PlayerSettingsAction
    data class SetDanmakuEnabled(val enabled: Boolean) : PlayerSettingsAction
    data class SetDanmakuSpeed(val speed: Float) : PlayerSettingsAction
    data class SetDanmakuSize(val size: Float) : PlayerSettingsAction
    data class SetDefaultQuality(val quality: String) : PlayerSettingsAction
    data class SetDefaultPlaybackSpeed(val speed: Double) : PlayerSettingsAction
    data class SetDefaultService(val service: Int) : PlayerSettingsAction
    data class SetSubtitlesEnabled(val enabled: Boolean) : PlayerSettingsAction
    data class SetSubtitleLanguage(val language: String) : PlayerSettingsAction
    data class SetAudioLanguage(val language: String) : PlayerSettingsAction
    data class SetPreferOriginalLanguage(val enabled: Boolean) : PlayerSettingsAction
    data class SetSponsorBlockMode(val mode: SponsorBlockMode) : PlayerSettingsAction
    data class SetSponsorBlockCategory(
        val category: String,
        val mode: SponsorBlockMode,
    ) : PlayerSettingsAction
    data class SetSponsorBlockMinimumDuration(val seconds: Int) : PlayerSettingsAction
    data class SetSponsorBlockOption(
        val option: SponsorBlockOption,
        val enabled: Boolean,
    ) : PlayerSettingsAction
}

enum class SponsorBlockOption {
    ShowCurrentSegment,
    ShowChapters,
    ShowFullVideoLabels,
    ManualSkipOnFullVideo,
    SkipNonMusicOnlyOnMusicVideos,
    MuteInsteadOfSkip,
}
