package dev.typetype.android.data.usersettings

import dev.typetype.android.data.account.AccountScope
import dev.typetype.android.data.network.TypeTypeApiHolder
import dev.typetype.android.data.network.dto.UserSettingsDto
import dev.typetype.android.data.network.requireSuccessfulResponse
import dev.typetype.android.domain.usersettings.CaptionStyles
import dev.typetype.android.domain.usersettings.DEFAULT_SPONSOR_BLOCK_CATEGORY_ACTIONS
import dev.typetype.android.domain.usersettings.SponsorBlockMode
import dev.typetype.android.domain.usersettings.UserSettings
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject

@Singleton
class UserSettingsNetworkSource @Inject constructor(
    private val apiHolder: TypeTypeApiHolder,
) {
    suspend fun fetch(scope: AccountScope): UserSettings = withContext(Dispatchers.IO) {
        val response = apiHolder.require(scope).settings()
        response.requireSuccessfulResponse()
        response.body()?.toDomain() ?: error("Empty settings response")
    }

    suspend fun update(scope: AccountScope, patch: JsonObject): UserSettings =
        withContext(Dispatchers.IO) {
            val response = apiHolder.require(scope).updateSettings(patch)
            response.requireSuccessfulResponse()
            response.body()?.toDomain() ?: error("Empty settings response")
        }
}

internal fun UserSettingsDto.toDomain(): UserSettings = UserSettings(
    defaultService = defaultService,
    defaultQuality = defaultQuality,
    defaultPlaybackSpeed = defaultPlaybackSpeed,
    defaultLandingPage = defaultLandingPage,
    autoplay = autoplay,
    skipPlaylistAutoplayScreen = skipPlaylistAutoplayScreen,
    volume = volume,
    muted = muted,
    subtitlesEnabled = subtitlesEnabled,
    defaultSubtitleLanguage = defaultSubtitleLanguage,
    defaultAudioLanguage = defaultAudioLanguage,
    captionStyles = CaptionStyles(
        fontFamily = captionStyles.fontFamily,
        fontSize = captionStyles.fontSize,
        textColor = captionStyles.textColor,
        textOpacity = captionStyles.textOpacity,
        textShadow = captionStyles.textShadow,
        textBackground = captionStyles.textBg,
        textBackgroundOpacity = captionStyles.textBgOpacity,
        displayBackground = captionStyles.displayBg,
        displayBackgroundOpacity = captionStyles.displayBgOpacity,
    ),
    preferOriginalLanguage = preferOriginalLanguage,
    enableHighQualityPlayback = enableHighQualityPlayback,
    sponsorBlockMode = SponsorBlockMode.fromWireValue(sponsorBlockMode),
    sponsorBlockCategoryActions = DEFAULT_SPONSOR_BLOCK_CATEGORY_ACTIONS +
        sponsorBlockCategoryActions.mapValues { SponsorBlockMode.fromWireValue(it.value) },
    sponsorBlockMinimumDuration = sponsorBlockMinimumDuration,
    sponsorBlockShowCurrentSegment = sponsorBlockShowCurrentSegment,
    sponsorBlockShowChapters = sponsorBlockShowChapters,
    sponsorBlockShowFullVideoLabels = sponsorBlockShowFullVideoLabels,
    sponsorBlockManualSkipOnFullVideo = sponsorBlockManualSkipOnFullVideo,
    sponsorBlockSkipNonMusicOnlyOnMusicVideos = sponsorBlockSkipNonMusicOnlyOnMusicVideos,
    sponsorBlockMuteInsteadOfSkip = sponsorBlockMuteInsteadOfSkip,
    hideHomeRecommendations = hideHomeRecommendations,
    hideContinueWatching = hideContinueWatching,
    hideRelatedVideos = hideRelatedVideos,
    hideComments = hideComments,
    hideShorts = hideShorts,
    disableWatchHistory = disableWatchHistory,
    deArrowEnabled = deArrowEnabled,
    deArrowTitleMode = deArrowTitleMode,
    deArrowThumbnailMode = deArrowThumbnailMode,
    deArrowTrustMode = deArrowTrustMode,
    accessMode = accessMode,
)
