package dev.typetype.android.domain.push

import dev.typetype.android.domain.server.PushCapability

interface PushRepository {
    suspend fun currentCapability(): PushCapability

    suspend fun registerDevice(deviceId: String, endpoint: String): Result<Unit>

    suspend fun unregisterDevice(deviceId: String): Result<Unit>

    suspend fun devices(): Result<List<PushDevice>>

    suspend fun channelPreferences(): Result<List<ChannelNotificationsPreference>>

    suspend fun setChannelPreference(channelUrl: String, enabled: Boolean): Result<Unit>
}
