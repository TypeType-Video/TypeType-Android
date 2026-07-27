package dev.typetype.android.data.library.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.typetype.android.data.account.AccountDao
import dev.typetype.android.data.account.AccountScope
import dev.typetype.android.data.library.LibraryNetworkSource
import dev.typetype.android.data.usersettings.UserSettingsNetworkSource
import dev.typetype.android.domain.server.ServerRepository
import kotlinx.coroutines.CancellationException

@HiltWorker
class ProgressSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val accountDao: AccountDao,
    private val outboxDao: ProgressOutboxDao,
    private val network: LibraryNetworkSource,
    private val serverRepository: ServerRepository,
    private val userSettingsNetwork: UserSettingsNetworkSource,
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val scope = inputScope() ?: return Result.failure()
        val baseUrl = inputData.getString(ProgressSyncWorkContract.BASE_URL)
            ?: return Result.failure()
        val generation = inputData.getLong(ProgressSyncWorkContract.SESSION_GENERATION, -1L)
        if (!isCurrent(scope, baseUrl, generation)) return Result.failure()
        return try {
            drain(scope, baseUrl, generation)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            if (shouldRetryProgressSync(failure)) Result.retry() else Result.failure()
        }
    }

    private suspend fun drain(scope: AccountScope, baseUrl: String, generation: Long): Result {
        repeat(MAX_BATCHES) {
            if (isStopped) return Result.retry()
            if (!isCurrent(scope, baseUrl, generation)) return Result.failure()
            val pending = outboxDao.pending(scope.serverId, scope.accountId, generation, BATCH_SIZE)
            if (pending.isEmpty()) return Result.success()
            if (userSettingsNetwork.fetch(scope).disableWatchHistory) {
                outboxDao.deleteGeneration(scope.serverId, scope.accountId, generation)
                return Result.success()
            }
            pending.forEach { entry ->
                network.putProgress(scope, entry.videoUrl, entry.positionMillis)
                outboxDao.deleteIfUnchanged(
                    entry.serverId,
                    entry.accountId,
                    entry.videoUrl,
                    entry.sessionGeneration,
                    entry.updatedAtMillis,
                )
            }
        }
        return Result.retry()
    }

    private fun inputScope(): AccountScope? {
        val serverId = inputData.getString(ProgressSyncWorkContract.SERVER_ID) ?: return null
        val accountId = inputData.getString(ProgressSyncWorkContract.ACCOUNT_ID) ?: return null
        return AccountScope(serverId, accountId)
    }

    private suspend fun isCurrent(scope: AccountScope, baseUrl: String, generation: Long): Boolean {
        val accountGeneration = accountDao.get(scope.serverId, scope.accountId)?.sessionGeneration
        val currentBaseUrl = serverRepository.getServer(scope.serverId)?.baseUrl
        return isProgressSyncTargetCurrent(
            accountGeneration,
            currentBaseUrl,
            generation,
            baseUrl,
        )
    }

    private companion object {
        const val BATCH_SIZE = 20
        const val MAX_BATCHES = 5
    }
}
