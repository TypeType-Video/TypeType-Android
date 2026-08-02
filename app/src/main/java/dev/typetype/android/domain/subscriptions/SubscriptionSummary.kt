package dev.typetype.android.domain.subscriptions

data class SubscriptionSummary(
    val channelUrl: String,
    val name: String,
    val avatarUrl: String,
    val subscribedAtMillis: Long,
)
