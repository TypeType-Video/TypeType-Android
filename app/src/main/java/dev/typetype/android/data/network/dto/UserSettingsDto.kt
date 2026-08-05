package dev.typetype.android.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserSettingsDto(
    val defaultService: Int = 0,
    val defaultQuality: String = "1080p",
    val defaultPlaybackSpeed: Double = 1.0,
    val defaultLandingPage: String = "home",
    val autoplay: Boolean = true,
    val skipPlaylistAutoplayScreen: Boolean = false,
    val volume: Double = 1.0,
    val muted: Boolean = false,
    val subtitlesEnabled: Boolean = false,
    val defaultSubtitleLanguage: String = "",
    val defaultAudioLanguage: String = "",
    val captionStyles: CaptionStylesDto = CaptionStylesDto(),
    val preferOriginalLanguage: Boolean = false,
    val enableHighQualityPlayback: Boolean = false,
    val sponsorBlockMode: String = "auto_skip",
    val sponsorBlockCategoryActions: Map<String, String> = emptyMap(),
    val sponsorBlockMinimumDuration: Int = 0,
    val sponsorBlockShowCurrentSegment: Boolean = true,
    val sponsorBlockShowChapters: Boolean = false,
    val sponsorBlockShowFullVideoLabels: Boolean = true,
    val sponsorBlockManualSkipOnFullVideo: Boolean = true,
    val sponsorBlockSkipNonMusicOnlyOnMusicVideos: Boolean = false,
    val sponsorBlockMuteInsteadOfSkip: Boolean = false,
    val hideHomeRecommendations: Boolean = false,
    val hideContinueWatching: Boolean = false,
    val hideRelatedVideos: Boolean = false,
    val hideComments: Boolean = false,
    val hideShorts: Boolean = false,
    val disableWatchHistory: Boolean = false,
    val deArrowEnabled: Boolean = false,
    val deArrowTitleMode: String = "dearrow",
    val deArrowThumbnailMode: String = "dearrow_or_random",
    val deArrowTrustMode: String = "accepted",
    val accessMode: String = "unrestricted",
)

@Serializable
data class CaptionStylesDto(
    val fontFamily: String = "",
    val fontSize: String = "",
    val textColor: String = "",
    val textOpacity: String = "",
    val textShadow: String = "",
    val textBg: String = "",
    val textBgOpacity: String = "",
    val displayBg: String = "",
    val displayBgOpacity: String = "",
)
