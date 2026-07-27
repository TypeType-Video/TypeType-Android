package dev.typetype.android.data.library

import androidx.room.withTransaction
import dev.typetype.android.data.account.AccountDao
import dev.typetype.android.data.account.AccountScope
import dev.typetype.android.data.database.TypeTypeDatabase
import dev.typetype.android.data.library.local.HistoryDao
import dev.typetype.android.data.library.sync.ProgressOutboxDao
import dev.typetype.android.data.library.sync.ProgressOutboxEntity
import dev.typetype.android.data.library.sync.ProgressSyncScheduler
import dev.typetype.android.domain.server.ServerRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibraryProgressSync @Inject constructor(
    private val accountDao: AccountDao,
    private val historyDao: HistoryDao,
    private val outboxDao: ProgressOutboxDao,
    private val scheduler: ProgressSyncScheduler,
    private val network: LibraryNetworkSource,
    private val serverRepository: ServerRepository,
    private val database: TypeTypeDatabase,
) {
    suspend fun save(scope: AccountScope, videoUrl: String, positionMillis: Long) {
        val account = accountDao.get(scope.serverId, scope.accountId) ?: error("Account not found")
        val server = serverRepository.getServer(scope.serverId) ?: error("Instance not found")
        val now = System.currentTimeMillis()
        database.withTransaction {
            historyDao.updateProgress(
                serverId = scope.serverId,
                accountId = scope.accountId,
                url = videoUrl,
                seconds = positionMillis / 1_000L,
                watchedAtMillis = now,
            )
            outboxDao.deleteStale(scope.serverId, scope.accountId, account.sessionGeneration)
            outboxDao.upsert(
                ProgressOutboxEntity(
                    serverId = scope.serverId,
                    accountId = scope.accountId,
                    videoUrl = videoUrl,
                    positionMillis = positionMillis,
                    sessionGeneration = account.sessionGeneration,
                    updatedAtMillis = now,
                ),
            )
        }
        scheduler.enqueue(scope, server.baseUrl, account.sessionGeneration)
    }

    suspend fun fetch(scope: AccountScope, videoUrl: String): Long? {
        val generation = accountDao.get(scope.serverId, scope.accountId)?.sessionGeneration
            ?: error("Account not found")
        val pending = outboxDao.get(scope.serverId, scope.accountId, videoUrl)
            ?.takeIf { it.sessionGeneration == generation }
        if (pending != null) return pending.positionMillis
        val local = historyDao.getProgressSeconds(scope.serverId, scope.accountId, videoUrl)
            ?.times(1_000L)
        return try {
            network.getProgress(scope, videoUrl) ?: local
        } catch (failure: Exception) {
            local ?: throw failure
        }
    }

    suspend fun discardPending(scope: AccountScope) {
        outboxDao.deleteAllForScope(scope.serverId, scope.accountId)
    }
}
