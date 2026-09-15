package dev.typetype.android.data.network.dto

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
