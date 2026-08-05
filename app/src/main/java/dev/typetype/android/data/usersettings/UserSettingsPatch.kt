package dev.typetype.android.data.usersettings

import dev.typetype.android.domain.usersettings.UserSettings
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal fun UserSettings.patchFrom(previous: UserSettings): JsonObject = buildJsonObject {
    if (defaultService != previous.defaultService) put("defaultService", defaultService)
    if (defaultQuality != previous.defaultQuality) put("defaultQuality", defaultQuality)
    if (defaultPlaybackSpeed != previous.defaultPlaybackSpeed) {
        put("defaultPlaybackSpeed", defaultPlaybackSpeed)
    }
    if (defaultLandingPage != previous.defaultLandingPage) {
        put("defaultLandingPage", defaultLandingPage)
    }
    if (autoplay != previous.autoplay) put("autoplay", autoplay)
    if (skipPlaylistAutoplayScreen != previous.skipPlaylistAutoplayScreen) {
        put("skipPlaylistAutoplayScreen", skipPlaylistAutoplayScreen)
    }
    if (volume != previous.volume) put("volume", volume)
    if (muted != previous.muted) put("muted", muted)
    if (subtitlesEnabled != previous.subtitlesEnabled) put("subtitlesEnabled", subtitlesEnabled)
    if (defaultSubtitleLanguage != previous.defaultSubtitleLanguage) {
        put("defaultSubtitleLanguage", defaultSubtitleLanguage)
    }
    if (defaultAudioLanguage != previous.defaultAudioLanguage) {
        put("defaultAudioLanguage", defaultAudioLanguage)
    }
    if (captionStyles != previous.captionStyles) {
        put("captionStyles", buildJsonObject {
            put("fontFamily", captionStyles.fontFamily)
            put("fontSize", captionStyles.fontSize)
            put("textColor", captionStyles.textColor)
            put("textOpacity", captionStyles.textOpacity)
            put("textShadow", captionStyles.textShadow)
            put("textBg", captionStyles.textBackground)
            put("textBgOpacity", captionStyles.textBackgroundOpacity)
            put("displayBg", captionStyles.displayBackground)
            put("displayBgOpacity", captionStyles.displayBackgroundOpacity)
        })
    }
    if (preferOriginalLanguage != previous.preferOriginalLanguage) {
        put("preferOriginalLanguage", preferOriginalLanguage)
    }
    if (enableHighQualityPlayback != previous.enableHighQualityPlayback) {
        put("enableHighQualityPlayback", enableHighQualityPlayback)
    }
    if (sponsorBlockMode != previous.sponsorBlockMode) {
        put("sponsorBlockMode", sponsorBlockMode.wireValue)
    }
    if (sponsorBlockCategoryActions != previous.sponsorBlockCategoryActions) {
        put("sponsorBlockCategoryActions", buildJsonObject {
            sponsorBlockCategoryActions.forEach { (category, action) ->
                put(category, action.wireValue)
            }
        })
    }
    if (sponsorBlockMinimumDuration != previous.sponsorBlockMinimumDuration) {
        put("sponsorBlockMinimumDuration", sponsorBlockMinimumDuration)
    }
    if (sponsorBlockShowCurrentSegment != previous.sponsorBlockShowCurrentSegment) {
        put("sponsorBlockShowCurrentSegment", sponsorBlockShowCurrentSegment)
    }
    if (sponsorBlockShowChapters != previous.sponsorBlockShowChapters) {
        put("sponsorBlockShowChapters", sponsorBlockShowChapters)
    }
    if (sponsorBlockShowFullVideoLabels != previous.sponsorBlockShowFullVideoLabels) {
        put("sponsorBlockShowFullVideoLabels", sponsorBlockShowFullVideoLabels)
    }
    if (sponsorBlockManualSkipOnFullVideo != previous.sponsorBlockManualSkipOnFullVideo) {
        put("sponsorBlockManualSkipOnFullVideo", sponsorBlockManualSkipOnFullVideo)
    }
    if (
        sponsorBlockSkipNonMusicOnlyOnMusicVideos !=
        previous.sponsorBlockSkipNonMusicOnlyOnMusicVideos
    ) {
        put(
            "sponsorBlockSkipNonMusicOnlyOnMusicVideos",
            sponsorBlockSkipNonMusicOnlyOnMusicVideos,
        )
    }
    if (sponsorBlockMuteInsteadOfSkip != previous.sponsorBlockMuteInsteadOfSkip) {
        put("sponsorBlockMuteInsteadOfSkip", sponsorBlockMuteInsteadOfSkip)
    }
    if (hideHomeRecommendations != previous.hideHomeRecommendations) {
        put("hideHomeRecommendations", hideHomeRecommendations)
    }
    if (hideContinueWatching != previous.hideContinueWatching) {
        put("hideContinueWatching", hideContinueWatching)
    }
    if (hideRelatedVideos != previous.hideRelatedVideos) {
        put("hideRelatedVideos", hideRelatedVideos)
    }
    if (hideComments != previous.hideComments) put("hideComments", hideComments)
    if (hideShorts != previous.hideShorts) put("hideShorts", hideShorts)
    if (disableWatchHistory != previous.disableWatchHistory) {
        put("disableWatchHistory", disableWatchHistory)
    }
    if (deArrowEnabled != previous.deArrowEnabled) put("deArrowEnabled", deArrowEnabled)
    if (deArrowTitleMode != previous.deArrowTitleMode) {
        put("deArrowTitleMode", deArrowTitleMode)
    }
    if (deArrowThumbnailMode != previous.deArrowThumbnailMode) {
        put("deArrowThumbnailMode", deArrowThumbnailMode)
    }
    if (deArrowTrustMode != previous.deArrowTrustMode) {
        put("deArrowTrustMode", deArrowTrustMode)
    }
    if (accessMode != previous.accessMode) put("accessMode", accessMode)
}
