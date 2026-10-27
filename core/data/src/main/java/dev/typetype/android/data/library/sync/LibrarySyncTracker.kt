package dev.typetype.android.data.library.sync

import dev.typetype.android.core.error.CodedFailure
import dev.typetype.android.data.account.AccountScope
import dev.typetype.android.data.account.AccountDao
import dev.typetype.android.data.account.ActiveAccountScope
import dev.typetype.android.domain.library.LibraryCollection
import dev.typetype.android.domain.library.LibraryCollectionSyncState
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine

@OptIn(ExperimentalCoroutinesApi::class)
class LibrarySyncTracker @Inject constructor(
    private val dao: LibrarySyncDao,
    private val mutationDao: LibraryMutationDao,
    private val accountDao: AccountDao,
    activeAccountScope: ActiveAccountScope,
) {
    private val activeScope = activeAccountScope.observe()

    fun observe(): Flow<Map<LibraryCollection, LibraryCollectionSyncState>> =
        activeScope.flatMapLatest { scope ->
            if (scope == null) flowOf(emptyMap())
            else combine(
                dao.observe(scope.serverId, scope.accountId),
                mutationDao.observe(scope.serverId, scope.accountId),
                accountDao.observeSessionGeneration(scope.serverId, scope.accountId),
            ) { refreshRows, mutationRows, generation ->
                mergeSyncStates(
                    refreshRows,
                    mutationRows.filter { it.sessionGeneration == generation },
                )
            }
        }

    internal suspend fun begin(scope: AccountScope, collection: LibraryCollection): LibraryRefreshToken {
        val generation = dao.begin(
            serverId = scope.serverId,
            accountId = scope.accountId,
            collection = collection.storageKey,
            attemptedAtMillis = System.currentTimeMillis(),
        )
        return LibraryRefreshToken(scope, collection, generation)
    }

    internal suspend fun succeed(token: LibraryRefreshToken) {
        dao.completeSuccess(
            serverId = token.scope.serverId,
            accountId = token.scope.accountId,
            collection = token.collection.storageKey,
            generation = token.generation,
            completedAtMillis = System.currentTimeMillis(),
        )
    }

    internal suspend fun isCurrent(token: LibraryRefreshToken): Boolean =
        dao.isCurrent(
            serverId = token.scope.serverId,
            accountId = token.scope.accountId,
            collection = token.collection.storageKey,
            generation = token.generation,
        )

    internal suspend fun fail(token: LibraryRefreshToken, failure: Throwable) {
        val snapshot = syncFailureSnapshot(failure)
        dao.completeFailure(
            serverId = token.scope.serverId,
            accountId = token.scope.accountId,
            collection = token.collection.storageKey,
            generation = token.generation,
            completedAtMillis = System.currentTimeMillis(),
            failureCode = snapshot.code,
            failureStatusCode = snapshot.statusCode,
            requestId = snapshot.requestId,
        )
    }
}

internal fun mergeSyncStates(
    refreshRows: List<LibrarySyncEntity>,
    mutationRows: List<LibraryMutationEntity>,
): Map<LibraryCollection, LibraryCollectionSyncState> {
    val refresh = refreshRows.mapNotNull { it.toDomainOrNull() }.associateBy { it.collection }
    val writes = mutationRows.groupBy { row ->
        LibraryCollection.entries.firstOrNull { it.storageKey == row.collection }
    }.filterKeys { it != null }
    return (refresh.keys + writes.keys.filterNotNull()).associateWith { collection ->
        val rows = writes[collection].orEmpty()
        val failed = rows.filter { it.state == MUTATION_FAILED }.maxByOrNull { it.updatedAtMillis }
        val base = refresh[collection] ?: LibraryCollectionSyncState(
            collection = collection,
            lastAttemptAtMillis = rows.maxOfOrNull { it.updatedAtMillis } ?: 0L,
            lastSuccessAtMillis = null,
            lastFailureAtMillis = null,
            failureCode = null,
            failureStatusCode = null,
            requestId = null,
        )
        base.copy(
            pendingWriteCount = rows.count { it.state == MUTATION_PENDING },
            failedWriteCount = rows.count { it.state == MUTATION_FAILED },
            writeFailureCode = failed?.failureCode,
            writeFailureStatusCode = failed?.failureStatusCode,
            writeRequestId = failed?.requestId,
        )
    }
}

internal data class LibraryRefreshToken(
    val scope: AccountScope,
    val collection: LibraryCollection,
    val generation: Long,
)

internal data class SyncFailureSnapshot(
    val code: String?,
    val statusCode: Int?,
    val requestId: String?,
)

internal fun syncFailureSnapshot(failure: Throwable): SyncFailureSnapshot {
    val coded = failure as? CodedFailure
    val code = coded?.failureCode?.takeIf(SAFE_CODE::matches)
        ?: CLIENT_NETWORK_CODE.takeIf { failure is IOException }
    return SyncFailureSnapshot(
        code = code,
        statusCode = coded?.statusCode,
        requestId = coded?.requestId?.takeIf(SAFE_REQUEST_ID::matches),
    )
}

private fun LibrarySyncEntity.toDomainOrNull(): LibraryCollectionSyncState? {
    val kind = LibraryCollection.entries.firstOrNull { it.storageKey == collection } ?: return null
    return LibraryCollectionSyncState(
        collection = kind,
        lastAttemptAtMillis = lastAttemptAtMillis,
        lastSuccessAtMillis = lastSuccessAtMillis,
        lastFailureAtMillis = lastFailureAtMillis,
        failureCode = failureCode,
        failureStatusCode = failureStatusCode,
        requestId = requestId,
    )
}

private val SAFE_CODE = Regex("[A-Za-z][A-Za-z0-9_.:-]{0,127}")
private val SAFE_REQUEST_ID = Regex("[A-Za-z0-9._:-]{1,128}")
private const val CLIENT_NETWORK_CODE = "client_network_unavailable"
