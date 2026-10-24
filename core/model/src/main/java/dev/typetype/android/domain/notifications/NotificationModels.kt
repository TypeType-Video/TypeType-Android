package dev.typetype.android.domain.notifications

import dev.typetype.android.domain.feed.Video

data class NotificationItem(
    val type: String,
    val title: String,
    val createdAtMillis: Long,
    val publishedAtMillis: Long,
    val channelUrl: String,
    val channelName: String,
    val channelAvatarUrl: String,
    val video: Video,
)

data class NotificationsPage(
    val items: List<NotificationItem>,
    val unreadCount: Int,
    val nextPage: Int?,
)

data class NotificationBadge(
    val isAvailable: Boolean = false,
    val unreadCount: Int = 0,
)
