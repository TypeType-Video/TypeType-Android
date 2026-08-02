package dev.typetype.android.domain.subscriptions

import kotlinx.coroutines.flow.Flow
import dev.typetype.android.domain.library.LibraryCollectionSyncState

interface SubscriptionsRepository {
    fun observeSubscribedChannelUrls(): Flow<Set<String>>
    fun observeSubscriptions(): Flow<List<SubscriptionSummary>>
    fun observeSyncState(): Flow<LibraryCollectionSyncState?>
    suspend fun refresh(): Result<Unit>
    suspend fun listSubscriptions(): Result<List<SubscriptionSummary>>
    suspend fun subscribe(
        channelUrl: String,
        name: String,
        avatarUrl: String,
    ): Result<Unit>
    suspend fun unsubscribe(channelUrl: String): Result<Unit>
    suspend fun unsubscribeAll(): Result<Unit>
    suspend fun retryPendingWrites(): Result<Boolean>
}
