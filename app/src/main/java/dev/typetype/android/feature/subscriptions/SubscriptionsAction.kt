package dev.typetype.android.feature.subscriptions

sealed interface SubscriptionsAction {
    data object OnRefresh : SubscriptionsAction
    data object OnLoadMore : SubscriptionsAction
    data object OnRetrySync : SubscriptionsAction
}
