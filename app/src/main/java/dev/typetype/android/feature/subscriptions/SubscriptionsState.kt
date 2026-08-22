package dev.typetype.android.feature.subscriptions

import dev.typetype.android.domain.feed.Video
import dev.typetype.android.domain.subscriptions.SubscriptionSummary

enum class SubscriptionsTab { Videos, Channels }

data class SubscriptionsState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isServerRefreshing: Boolean = false,
    val videos: List<Video> = emptyList(),
    val channels: List<SubscriptionSummary> = emptyList(),
    val selectedTab: SubscriptionsTab = SubscriptionsTab.Videos,
    val hasMore: Boolean = true,
    val generatedAtMillis: Long? = null,
    val loadMoreError: Boolean = false,
    val errorMessage: String? = null,
    val errorRequestId: String? = null,
    val syncErrorMessage: String? = null,
    val syncRequestId: String? = null,
    val lastSuccessfulSyncAtMillis: Long? = null,
    val pendingWriteCount: Int = 0,
    val failedWriteCount: Int = 0,
)
