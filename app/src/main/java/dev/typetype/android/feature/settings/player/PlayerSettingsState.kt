package dev.typetype.android.feature.settings.player

import dev.typetype.android.domain.usersettings.DEFAULT_SPONSOR_BLOCK_CATEGORY_ACTIONS
import dev.typetype.android.domain.usersettings.SponsorBlockMode

data class PlayerSettingsState(
    val doubleTapSeekEnabled: Boolean = true,
    val swipeSeekEnabled: Boolean = true,
    val swipeBrightnessVolumeEnabled: Boolean = true,
    val longPressSpeedEnabled: Boolean = true,
    val autoplayEnabled: Boolean = true,
    val autoplayCountdownSeconds: Int = 10,
    val skipPlaylistAutoplayScreen: Boolean = false,
    val pauseInBackgroundEnabled: Boolean = false,
    val defaultQuality: String = "1080p",
    val defaultPlaybackSpeed: Double = 1.0,
    val defaultService: Int = 0,
    val subtitlesEnabled: Boolean = false,
    val defaultSubtitleLanguage: String = "",
    val defaultAudioLanguage: String = "",
    val preferOriginalLanguage: Boolean = false,
    val sponsorBlockMode: SponsorBlockMode = SponsorBlockMode.AutoSkip,
    val sponsorBlockCategoryActions: Map<String, SponsorBlockMode> =
        DEFAULT_SPONSOR_BLOCK_CATEGORY_ACTIONS,
    val sponsorBlockMinimumDuration: Int = 0,
    val sponsorBlockShowCurrentSegment: Boolean = true,
    val sponsorBlockShowChapters: Boolean = false,
    val sponsorBlockShowFullVideoLabels: Boolean = true,
    val sponsorBlockManualSkipOnFullVideo: Boolean = true,
    val sponsorBlockSkipNonMusicOnlyOnMusicVideos: Boolean = false,
    val sponsorBlockMuteInsteadOfSkip: Boolean = false,
)
