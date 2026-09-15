package dev.typetype.android.domain.push

data class PushDevice(
    val deviceId: String,
    val updatedAt: Long,
)

data class ChannelNotificationsPreference(
    val channelUrl: String,
    val enabled: Boolean,
)

sealed interface PushRegistrationStatus {
    data object Disabled : PushRegistrationStatus

    data object Unavailable : PushRegistrationStatus

    data object MissingDistributor : PushRegistrationStatus

    data object Registering : PushRegistrationStatus

    data object Registered : PushRegistrationStatus

    data object Failed : PushRegistrationStatus
}
