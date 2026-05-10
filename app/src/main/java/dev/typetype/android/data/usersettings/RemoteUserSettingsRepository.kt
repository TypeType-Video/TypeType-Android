package dev.typetype.android.data.usersettings

import dev.typetype.android.data.network.TypeTypeApiHolder
import dev.typetype.android.data.network.dto.UserSettingsDto
import dev.typetype.android.data.network.extractServerErrorMessage
import dev.typetype.android.domain.usersettings.UserSettings
import dev.typetype.android.domain.usersettings.UserSettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Singleton
class RemoteUserSettingsRepository @Inject constructor(
    private val apiHolder: TypeTypeApiHolder,
) : UserSettingsRepository {

    private val state = MutableStateFlow(UserSettings())
    private val mutex = Mutex()

    override fun observe(): Flow<UserSettings> = state.asStateFlow()

    override suspend fun refresh(): Result<Unit> = runCatching {
        val response = withContext(Dispatchers.IO) { apiHolder.require().settings() }
        if (!response.isSuccessful) error(extractServerErrorMessage(response))
        state.value = response.body()?.toDomain() ?: UserSettings()
    }

    override suspend fun update(transform: (UserSettings) -> UserSettings): Result<Unit> =
        mutex.withLock {
            runCatching {
                val previous = state.value
                val next = transform(previous)
                state.value = next
                val response = withContext(Dispatchers.IO) {
                    apiHolder.require().updateSettings(next.toDto())
                }
                if (!response.isSuccessful) {
                    state.value = previous
                    error(extractServerErrorMessage(response))
                }
                val refreshed = response.body()?.toDomain()
                if (refreshed != null) state.value = refreshed
            }
        }

    private fun UserSettingsDto.toDomain(): UserSettings = UserSettings(
        defaultService = defaultService,
        defaultQuality = defaultQuality,
        autoplay = autoplay,
        volume = volume,
        muted = muted,
        subtitlesEnabled = subtitlesEnabled,
        defaultSubtitleLanguage = defaultSubtitleLanguage,
        defaultAudioLanguage = defaultAudioLanguage,
        preferOriginalLanguage = preferOriginalLanguage,
    )

    private fun UserSettings.toDto(): UserSettingsDto = UserSettingsDto(
        defaultService = defaultService,
        defaultQuality = defaultQuality,
        autoplay = autoplay,
        volume = volume,
        muted = muted,
        subtitlesEnabled = subtitlesEnabled,
        defaultSubtitleLanguage = defaultSubtitleLanguage,
        defaultAudioLanguage = defaultAudioLanguage,
        preferOriginalLanguage = preferOriginalLanguage,
    )
}
