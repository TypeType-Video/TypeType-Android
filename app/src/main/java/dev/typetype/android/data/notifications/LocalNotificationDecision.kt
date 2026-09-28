package dev.typetype.android.data.notifications

import dev.typetype.android.domain.notifications.NotificationItem
import dev.typetype.android.domain.push.ChannelNotificationsPreference
import dev.typetype.android.domain.push.matchesChannel

internal data class LocalNotificationDecision(
    val keysToRecord: List<String>,
    val notified: List<NotificationItem>,
)

internal fun decideLocalNotifications(
    items: List<NotificationItem>,
    knownKeys: Set<String>,
    seeded: Boolean,
    enabledChannels: List<ChannelNotificationsPreference>,
): LocalNotificationDecision {
    val fresh = items.filter { item -> item.localNotificationKey() !in knownKeys }
    if (!seeded) {
        return LocalNotificationDecision(
            keysToRecord = items.map { item -> item.localNotificationKey() },
            notified = emptyList(),
        )
    }
    if (fresh.isEmpty()) return LocalNotificationDecision(emptyList(), emptyList())
    val deliverableChannels = enabledChannels.filter { it.enabled }
    val deliverable = fresh
        .filter { item -> deliverableChannels.any { it.matchesChannel(item.channelUrl) } }
        .asReversed()
    return LocalNotificationDecision(
        keysToRecord = fresh.map { item -> item.localNotificationKey() },
        notified = deliverable,
    )
}

private fun NotificationItem.localNotificationKey(): String =
    video.id.takeIf { it.isNotBlank() } ?: video.url
