package dev.typetype.android.feature.subscriptions

sealed interface SubscriptionsAction {
    data object OnRefresh : SubscriptionsAction
}
