package dev.typetype.android.data.subscriptions

import androidx.room.withTransaction
import dev.typetype.android.data.account.AccountScope
import dev.typetype.android.data.account.ActiveAccountScope
import dev.typetype.android.data.database.TypeTypeDatabase
import dev.typetype.android.data.library.LibraryNetworkSource
import dev.typetype.android.data.library.sync.LibraryMutationKind
import dev.typetype.android.data.library.sync.LibraryMutationOverlay
import dev.typetype.android.data.library.sync.LibraryMutationRequest
import dev.typetype.android.data.library.sync.LibraryMutationWriter
import dev.typetype.android.data.library.sync.LibraryRefreshToken
import dev.typetype.android.data.library.sync.LibrarySyncTracker
import dev.typetype.android.domain.library.LibraryCollection
import dev.typetype.android.domain.library.LibraryCollectionSyncState
import dev.typetype.android.domain.subscriptions.SubscriptionSummary
import dev.typetype.android.domain.subscriptions.SubscriptionsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class RemoteSubscriptionsRepository @Inject constructor(
    private val network: LibraryNetworkSource,
    private val activeAccountScope: ActiveAccountScope,
    private val dao: SubscriptionDao,
    private val database: TypeTypeDatabase,
    private val mutationWriter: LibraryMutationWriter,
    private val mutationOverlay: LibraryMutationOverlay,
    private val syncTracker: LibrarySyncTracker,
) : SubscriptionsRepository {

    override fun observeSubscribedChannelUrls(): Flow<Set<String>> = scopedRows()
        .map { rows -> rows.map { it.channelUrl }.toSet() }

    override fun observeSyncState(): Flow<LibraryCollectionSyncState?> = syncTracker.observe()
        .map { it[LibraryCollection.Subscriptions] }

    override suspend fun refresh(): Result<Unit> {
        val scope = try {
            activeAccountScope.require()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Throwable) {
            return Result.failure(failure)
        }
        val token = syncTracker.begin(scope, LibraryCollection.Subscriptions)
        return try {
            val rows = network.fetchSubscriptions(scope).map { dto ->
                SubscriptionEntity(
                    serverId = scope.serverId,
                    accountId = scope.accountId,
                    channelUrl = dto.channelUrl,
                    name = dto.name,
                    avatarUrl = dto.avatarUrl,
                    subscribedAtMillis = dto.subscribedAt,
                )
            }
            activeAccountScope.verify(scope)
            database.withTransaction {
                if (!syncTracker.isCurrent(token)) throw SupersededSubscriptionRefresh()
                dao.replaceAll(scope.serverId, scope.accountId, rows)
                mutationOverlay.apply(scope, LibraryCollection.Subscriptions)
                syncTracker.succeed(token)
            }
            Result.success(Unit)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Throwable) {
            recordFailure(token, failure)
            Result.failure(failure)
        }
    }

    override suspend fun listSubscriptions(): Result<List<SubscriptionSummary>> = subscriptionResult {
        val scope = activeAccountScope.require()
        refresh().getOrThrow()
        dao.getAll(scope.serverId, scope.accountId).map { SubscriptionSummary(it.channelUrl) }
    }

    override suspend fun subscribe(
        channelUrl: String,
        name: String,
        avatarUrl: String,
    ): Result<Unit> = subscriptionResult {
        val scope = activeAccountScope.require()
        val normalizedUrl = normalizeChannelUrl(channelUrl)
        mutationWriter.enqueue(
            scope,
            LibraryMutationRequest(
                kind = LibraryMutationKind.Subscription,
                targetId = normalizedUrl,
                desiredPresent = true,
                title = name,
                thumbnailUrl = avatarUrl,
            ),
        ) {
            dao.upsert(
                SubscriptionEntity(
                    serverId = scope.serverId,
                    accountId = scope.accountId,
                    channelUrl = normalizedUrl,
                    name = name,
                    avatarUrl = avatarUrl,
                    subscribedAtMillis = System.currentTimeMillis(),
                ),
            )
        }
    }

    override suspend fun unsubscribe(channelUrl: String): Result<Unit> = subscriptionResult {
        val scope = activeAccountScope.require()
        enqueueRemoval(scope, channelUrl)
    }

    override suspend fun unsubscribeAll(): Result<Unit> = subscriptionResult {
        val scope = activeAccountScope.require()
        refresh().getOrThrow()
        dao.getAll(scope.serverId, scope.accountId).forEach { row ->
            activeAccountScope.verify(scope)
            enqueueRemoval(scope, row.channelUrl)
        }
    }

    override suspend fun retryPendingWrites(): Result<Boolean> = subscriptionResult {
        mutationWriter.retry(activeAccountScope.require(), LibraryCollection.Subscriptions)
    }

    private suspend fun enqueueRemoval(scope: AccountScope, channelUrl: String) {
        val normalizedUrl = normalizeChannelUrl(channelUrl)
        mutationWriter.enqueue(
            scope,
            LibraryMutationRequest(
                kind = LibraryMutationKind.Subscription,
                targetId = normalizedUrl,
                desiredPresent = false,
            ),
        ) {
            dao.delete(scope.serverId, scope.accountId, channelUrl)
            if (normalizedUrl != channelUrl) dao.delete(scope.serverId, scope.accountId, normalizedUrl)
        }
    }

    private fun scopedRows(): Flow<List<SubscriptionEntity>> =
        activeAccountScope.observe().flatMapLatest { scope ->
            if (scope == null) flowOf(emptyList()) else dao.observe(scope.serverId, scope.accountId)
        }

    private suspend fun recordFailure(token: LibraryRefreshToken, failure: Throwable) {
        val current = try {
            activeAccountScope.verify(token.scope)
            true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            false
        }
        if (current) syncTracker.fail(token, failure)
    }
}

private suspend fun <T> subscriptionResult(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (failure: Throwable) {
    Result.failure(failure)
}

private class SupersededSubscriptionRefresh : CancellationException("Subscription refresh was superseded")
