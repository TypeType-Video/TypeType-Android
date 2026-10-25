package dev.typetype.android.services.notifications

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
    nowMillis: Long,
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
        .filter { item -> item.isAlertWorthy(nowMillis) }
        .filter { item ->
            deliverableChannels.any { bell ->
                bell.matchesChannel(item.channelUrl) && item.publishedAtMillis >= bell.updatedAtMillis
            }
        }
        .asReversed()
    return LocalNotificationDecision(
        keysToRecord = fresh
            .filterNot { item -> item.publishedAtMillis > nowMillis + CLOCK_SKEW_MILLIS }
            .map { item -> item.localNotificationKey() },
        notified = deliverable,
    )
}

private fun NotificationItem.isAlertWorthy(nowMillis: Long): Boolean {
    val video = video
    if (video.requiresMembership || video.isShortFormContent) return false
    if (video.isLive || video.isLiveContent || video.isPostLive) return false
    return publishedAtMillis in (nowMillis - ALERT_WINDOW_MILLIS)..(nowMillis + CLOCK_SKEW_MILLIS)
}

internal fun NotificationItem.localNotificationKey(): String =
    video.id.takeIf { it.isNotBlank() } ?: video.url

internal fun LocalNotificationDecision.keysToPersist(unpostedKeys: Set<String>): List<String> =
    keysToRecord.filterNot(unpostedKeys::contains)

private const val ALERT_WINDOW_MILLIS = 24L * 60 * 60 * 1000
private const val CLOCK_SKEW_MILLIS = 5L * 60 * 1000
