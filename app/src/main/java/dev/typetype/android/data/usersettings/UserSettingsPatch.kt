package dev.typetype.android.data.usersettings

import dev.typetype.android.domain.usersettings.UserSettings
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal fun UserSettings.patchFrom(previous: UserSettings): JsonObject = buildJsonObject {
    if (defaultService != previous.defaultService) put("defaultService", defaultService)
    if (defaultQuality != previous.defaultQuality) put("defaultQuality", defaultQuality)
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
    if (preferOriginalLanguage != previous.preferOriginalLanguage) {
        put("preferOriginalLanguage", preferOriginalLanguage)
    }
    if (hideHomeRecommendations != previous.hideHomeRecommendations) {
        put("hideHomeRecommendations", hideHomeRecommendations)
    }
    if (hideContinueWatching != previous.hideContinueWatching) {
        put("hideContinueWatching", hideContinueWatching)
    }
    if (disableWatchHistory != previous.disableWatchHistory) {
        put("disableWatchHistory", disableWatchHistory)
    }
}
