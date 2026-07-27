package dev.typetype.android.data.library.sync

import androidx.room.withTransaction
import dev.typetype.android.data.account.AccountDao
import dev.typetype.android.data.account.AccountScope
import dev.typetype.android.data.database.TypeTypeDatabase
import dev.typetype.android.domain.library.LibraryCollection
import dev.typetype.android.domain.server.ServerRepository
import javax.inject.Inject

class LibraryMutationWriter @Inject constructor(
    private val accountDao: AccountDao,
    private val dao: LibraryMutationDao,
    private val scheduler: LibraryMutationScheduler,
    private val serverRepository: ServerRepository,
    private val database: TypeTypeDatabase,
) {
    suspend fun enqueue(
        scope: AccountScope,
        request: LibraryMutationRequest,
        applyLocal: suspend () -> Unit,
    ) {
        val account = accountDao.get(scope.serverId, scope.accountId) ?: error("Account not found")
        val server = serverRepository.getServer(scope.serverId) ?: error("Instance not found")
        val now = System.currentTimeMillis()
        database.withTransaction {
            dao.deleteStale(scope.serverId, scope.accountId, account.sessionGeneration)
            val key = libraryMutationKey(request.kind, request.parentId, request.targetId)
            val previous = dao.get(scope.serverId, scope.accountId, key)
            applyLocal()
            dao.upsert(
                LibraryMutationEntity(
                    serverId = scope.serverId,
                    accountId = scope.accountId,
                    mutationKey = key,
                    collection = request.kind.collection.storageKey,
                    kind = request.kind.storageKey,
                    targetId = request.targetId,
                    parentId = request.parentId,
                    desiredPresent = request.desiredPresent,
                    title = request.title,
                    thumbnailUrl = request.thumbnailUrl,
                    durationSeconds = request.durationSeconds,
                    channelName = request.channelName,
                    channelUrl = request.channelUrl,
                    channelAvatarUrl = request.channelAvatarUrl,
                    viewCount = request.viewCount,
                    sessionGeneration = account.sessionGeneration,
                    mutationVersion = (previous?.mutationVersion ?: 0L) + 1L,
                    state = MUTATION_PENDING,
                    createdAtMillis = previous?.createdAtMillis ?: now,
                    updatedAtMillis = now,
                    lastAttemptAtMillis = null,
                    attemptCount = 0,
                    failureCode = null,
                    failureStatusCode = null,
                    requestId = null,
                ),
            )
        }
        scheduler.enqueue(scope, server.baseUrl, account.sessionGeneration)
    }

    suspend fun retry(scope: AccountScope, collection: LibraryCollection): Boolean {
        val account = accountDao.get(scope.serverId, scope.accountId) ?: error("Account not found")
        val server = serverRepository.getServer(scope.serverId) ?: error("Instance not found")
        val changed = dao.retryFailed(
            scope.serverId,
            scope.accountId,
            collection.storageKey,
            System.currentTimeMillis(),
        )
        if (changed > 0) scheduler.enqueue(scope, server.baseUrl, account.sessionGeneration)
        return changed > 0
    }

    suspend fun resume(scope: AccountScope): Boolean {
        val account = accountDao.get(scope.serverId, scope.accountId) ?: error("Account not found")
        val server = serverRepository.getServer(scope.serverId) ?: error("Instance not found")
        dao.deleteStale(scope.serverId, scope.accountId, account.sessionGeneration)
        val pending = dao.pending(scope.serverId, scope.accountId, account.sessionGeneration, 1).isNotEmpty()
        if (pending) scheduler.enqueue(scope, server.baseUrl, account.sessionGeneration)
        return pending
    }
}

data class LibraryMutationRequest(
    val kind: LibraryMutationKind,
    val targetId: String,
    val parentId: String? = null,
    val desiredPresent: Boolean,
    val title: String = "",
    val thumbnailUrl: String = "",
    val durationSeconds: Long = 0L,
    val channelName: String = "",
    val channelUrl: String = "",
    val channelAvatarUrl: String = "",
    val viewCount: Long = 0L,
)
