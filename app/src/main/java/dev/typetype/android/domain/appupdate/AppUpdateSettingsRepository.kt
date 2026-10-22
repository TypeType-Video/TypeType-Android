package dev.typetype.android.domain.appupdate

import kotlinx.coroutines.flow.Flow

interface AppUpdateSettingsRepository {
    fun observeChannel(): Flow<AppUpdateChannel>
    suspend fun setChannel(channel: AppUpdateChannel)
}
