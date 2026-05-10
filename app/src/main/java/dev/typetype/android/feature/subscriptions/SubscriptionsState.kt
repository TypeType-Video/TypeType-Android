package dev.typetype.android.feature.subscriptions

import dev.typetype.android.domain.feed.Video

data class SubscriptionsState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val videos: List<Video> = emptyList(),
    val hasMore: Boolean = true,
    val errorMessage: String? = null,
)
