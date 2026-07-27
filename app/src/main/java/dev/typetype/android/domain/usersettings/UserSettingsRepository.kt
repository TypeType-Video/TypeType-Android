package dev.typetype.android.domain.usersettings

import kotlinx.coroutines.flow.Flow

interface UserSettingsRepository {
    fun observe(): Flow<UserSettings>
    suspend fun current(): Result<UserSettings>
    suspend fun refresh(): Result<Unit>
    suspend fun update(transform: (UserSettings) -> UserSettings): Result<Unit>
}
