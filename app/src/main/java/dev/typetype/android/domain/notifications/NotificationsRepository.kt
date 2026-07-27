package dev.typetype.android.domain.notifications

import kotlinx.coroutines.flow.Flow

interface NotificationsRepository {
    fun observeBadge(): Flow<NotificationBadge>
    suspend fun refreshUnreadCount(): Result<Unit>
    suspend fun page(page: Int = 0): Result<NotificationsPage>
    suspend fun markAllRead(): Result<Unit>
}
