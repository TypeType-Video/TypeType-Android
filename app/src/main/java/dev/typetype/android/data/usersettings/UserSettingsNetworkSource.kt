package dev.typetype.android.data.usersettings

import dev.typetype.android.data.account.AccountScope
import dev.typetype.android.data.network.TypeTypeApiHolder
import dev.typetype.android.data.network.dto.UserSettingsDto
import dev.typetype.android.data.network.requireSuccessfulResponse
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
    autoplay = autoplay,
    skipPlaylistAutoplayScreen = skipPlaylistAutoplayScreen,
    volume = volume,
    muted = muted,
    subtitlesEnabled = subtitlesEnabled,
    defaultSubtitleLanguage = defaultSubtitleLanguage,
    defaultAudioLanguage = defaultAudioLanguage,
    preferOriginalLanguage = preferOriginalLanguage,
    hideHomeRecommendations = hideHomeRecommendations,
    hideContinueWatching = hideContinueWatching,
    disableWatchHistory = disableWatchHistory,
)
