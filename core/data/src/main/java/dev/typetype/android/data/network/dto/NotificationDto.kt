package dev.typetype.android.data.network.dto

import dev.typetype.android.domain.notifications.NotificationItem
import dev.typetype.android.domain.notifications.NotificationsPage
import kotlinx.serialization.Serializable

@Serializable
data class NotificationItemDto(
    val type: String,
    val title: String,
    val createdAt: Long,
    val publishedAt: Long = createdAt,
    val channelUrl: String,
    val channelName: String,
    val channelAvatarUrl: String,
    val video: VideoItem,
)

@Serializable
data class NotificationsResponseDto(
    val items: List<NotificationItemDto> = emptyList(),
    val unreadCount: Int,
    val nextpage: String? = null,
)

@Serializable
data class UnreadNotificationsCountDto(
    val unreadCount: Int,
)

@Serializable
data class MarkNotificationsReadResponseDto(
    val readAt: Long,
    val unreadCount: Int,
)

internal fun NotificationsResponseDto.toDomain(): NotificationsPage = NotificationsPage(
    items = items.map(NotificationItemDto::toDomain),
    unreadCount = unreadCount.coerceAtLeast(0),
    nextPage = nextpage?.let {
        requireNotNull(it.toIntOrNull()?.takeIf { page -> page >= 0 }) {
            "The instance returned an invalid notifications page"
        }
    },
)

private fun NotificationItemDto.toDomain(): NotificationItem = NotificationItem(
    type = type,
    title = title,
    createdAtMillis = createdAt,
    publishedAtMillis = publishedAt,
    channelUrl = channelUrl,
    channelName = channelName,
    channelAvatarUrl = channelAvatarUrl,
    video = video.toDomainVideo(),
)
