package dev.typetype.android.services.push

internal data class PushDeliveryOutcome(
    val value: String,
    val reasonCode: String?,
)

internal fun NotificationPostResult.toPushDeliveryOutcome(): PushDeliveryOutcome = when (this) {
    NotificationPostResult.Posted -> PushDeliveryOutcome("posted", null)
    NotificationPostResult.NotificationsDisabled -> PushDeliveryOutcome("suppressed", "notifications_disabled")
    NotificationPostResult.ChannelBlocked -> PushDeliveryOutcome("suppressed", "channel_blocked")
    NotificationPostResult.PermissionDenied -> PushDeliveryOutcome("suppressed", "permission_denied")
    NotificationPostResult.Failed -> PushDeliveryOutcome("failed", "notification_manager_unavailable")
}
