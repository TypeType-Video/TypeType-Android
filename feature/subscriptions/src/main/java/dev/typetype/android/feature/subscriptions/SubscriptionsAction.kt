package dev.typetype.android.feature.subscriptions

import dev.typetype.android.domain.subscriptions.SubscriptionFeedSelection

sealed interface SubscriptionsAction {
    data object OnRefresh : SubscriptionsAction
    data object OnLoadMore : SubscriptionsAction
    data object OnRetrySync : SubscriptionsAction
    data class OnTabSelect(val tab: SubscriptionsTab) : SubscriptionsAction
    data class OnSelectFeed(val selection: SubscriptionFeedSelection) : SubscriptionsAction
}
