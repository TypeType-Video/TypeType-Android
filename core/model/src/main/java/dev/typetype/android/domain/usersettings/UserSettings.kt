package dev.typetype.android.domain.usersettings

data class UserSettings(
    val defaultService: Int = 0,
    val defaultQuality: String = "1080p",
    val defaultPlaybackSpeed: Double = 1.0,
    val holdPlaybackSpeed: Double = DEFAULT_HOLD_PLAYBACK_SPEED,
    val defaultLandingPage: String = "home",
    val autoplay: Boolean = true,
    val skipPlaylistAutoplayScreen: Boolean = false,
    val autoplayOnOpen: Boolean = true,
    val hideUpcomingPlaylistVideos: Boolean = false,
    val volume: Double = 1.0,
    val muted: Boolean = false,
    val subtitlesEnabled: Boolean = false,
    val defaultSubtitleLanguage: String = "",
    val defaultAudioLanguage: String = "",
    val captionStyles: CaptionStyles = CaptionStyles(),
    val preferOriginalLanguage: Boolean = false,
    val enableHighQualityPlayback: Boolean = false,
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
    val hideHomeRecommendations: Boolean = false,
    val hideContinueWatching: Boolean = false,
    val hideRelatedVideos: Boolean = false,
    val hideEndScreens: Boolean = false,
    val hideComments: Boolean = false,
    val hideShorts: Boolean = false,
    val hideMembersOnlyContent: Boolean = false,
    val hideSubscriptionLiveStreams: Boolean = false,
    val supportsHideSubscriptionLiveStreams: Boolean = false,
    val disableWatchHistory: Boolean = false,
    val deArrowEnabled: Boolean = false,
    val deArrowTitleMode: String = "dearrow",
    val deArrowThumbnailMode: String = "dearrow_or_random",
    val deArrowTrustMode: String = "accepted",
    val accessMode: String = "unrestricted",
    val seekbarIcon: String = SEEKBAR_ICON_TYPETYPE,
    val seekbarIconUrl: String? = null,
)

const val DEFAULT_HOLD_PLAYBACK_SPEED = 2.0
val HOLD_PLAYBACK_SPEEDS: List<Double> = listOf(1.5, 2.0, 2.5, 3.0, 4.0)

fun normalizedHoldPlaybackSpeed(value: Double): Double =
    value.takeIf { it in HOLD_PLAYBACK_SPEEDS } ?: DEFAULT_HOLD_PLAYBACK_SPEED

const val SEEKBAR_ICON_TYPETYPE = "typetype"
const val SEEKBAR_ICON_CUSTOM = "custom"
const val SEEKBAR_ICON_NONE = "none"
