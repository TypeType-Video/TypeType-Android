package dev.typetype.android.feature.notifications

import dev.typetype.android.domain.notifications.NotificationItem

data class NotificationsState(
    val items: List<NotificationItem> = emptyList(),
    val unreadCount: Int = 0,
    val nextPage: Int? = null,
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val isMarkingRead: Boolean = false,
    val errorMessage: String? = null,
    val errorRequestId: String? = null,
    val loadMoreError: Boolean = false,
    val actionErrorMessage: String? = null,
    val actionErrorRequestId: String? = null,
)

sealed interface NotificationsAction {
    data object Retry : NotificationsAction
    data object LoadMore : NotificationsAction
    data object MarkAllRead : NotificationsAction
}
