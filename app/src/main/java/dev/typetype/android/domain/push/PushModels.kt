package dev.typetype.android.domain.push

data class PushDevice(
    val deviceId: String,
    val updatedAt: Long,
)

data class ChannelNotificationsPreference(
    val channelUrl: String,
    val enabled: Boolean,
)

fun ChannelNotificationsPreference.matchesChannel(channelUrl: String): Boolean =
    this.channelUrl == channelUrl ||
        channelUrl.startsWith("${this.channelUrl}/") ||
        this.channelUrl.startsWith("$channelUrl/")

sealed interface PushRegistrationStatus {
    data object Disabled : PushRegistrationStatus

    data object Unavailable : PushRegistrationStatus

    data object MissingDistributor : PushRegistrationStatus

    data object Registering : PushRegistrationStatus

    data object Registered : PushRegistrationStatus

    data object Failed : PushRegistrationStatus
}
