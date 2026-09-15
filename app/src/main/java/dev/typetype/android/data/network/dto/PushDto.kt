package dev.typetype.android.data.network.dto

import dev.typetype.android.domain.push.ChannelNotificationsPreference
import dev.typetype.android.domain.push.PushDevice
import kotlinx.serialization.Serializable

@Serializable
data class PushDeviceRegistrationRequestDto(
    val deviceId: String,
    val platform: String = "android",
    val endpoint: String,
    val expiresAt: Long? = null,
)

@Serializable
data class PushDeviceRegistrationResponseDto(
    val id: String,
    val deviceId: String,
    val platform: String,
    val expiresAt: Long? = null,
    val updatedAt: Long,
)

@Serializable
data class ChannelNotificationPreferenceRequestDto(
    val channelUrl: String,
    val enabled: Boolean,
)

@Serializable
data class ChannelNotificationPreferenceDto(
    val channelUrl: String,
    val enabled: Boolean,
    val updatedAt: Long,
)

internal fun PushDeviceRegistrationResponseDto.toDomain(): PushDevice = PushDevice(
    deviceId = deviceId,
    updatedAt = updatedAt,
)

internal fun ChannelNotificationPreferenceDto.toDomain(): ChannelNotificationsPreference =
    ChannelNotificationsPreference(
        channelUrl = channelUrl,
        enabled = enabled,
    )
