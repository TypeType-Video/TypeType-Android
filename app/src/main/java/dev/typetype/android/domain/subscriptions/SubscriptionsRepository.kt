package dev.typetype.android.domain.subscriptions

import kotlinx.coroutines.flow.Flow

interface SubscriptionsRepository {
    fun observeSubscribedChannelUrls(): Flow<Set<String>>
    suspend fun refresh(): Result<Unit>
    suspend fun listSubscriptions(): Result<List<SubscriptionSummary>>
    suspend fun subscribe(
        channelUrl: String,
        name: String,
        avatarUrl: String,
    ): Result<Unit>
    suspend fun unsubscribe(channelUrl: String): Result<Unit>
    suspend fun unsubscribeAll(): Result<Unit>
}
