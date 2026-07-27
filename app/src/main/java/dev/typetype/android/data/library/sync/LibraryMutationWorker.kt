package dev.typetype.android.data.library.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.typetype.android.data.account.AccountDao
import dev.typetype.android.data.account.AccountScope
import dev.typetype.android.domain.server.ServerRepository
import java.io.IOException
import kotlinx.coroutines.CancellationException

@HiltWorker
class LibraryMutationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val accountDao: AccountDao,
    private val dao: LibraryMutationDao,
    private val remote: LibraryMutationRemote,
    private val serverRepository: ServerRepository,
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val target = inputTarget() ?: return Result.failure()
        if (!isCurrent(target)) return Result.failure()
        return try {
            drain(target)
        } catch (cancelled: CancellationException) {
            throw cancelled
        }
    }

    private suspend fun drain(target: MutationTarget): Result {
        repeat(MAX_BATCHES) {
            if (isStopped) return Result.retry()
            if (!isCurrent(target)) return Result.failure()
            val rows = dao.pending(
                target.scope.serverId,
                target.scope.accountId,
                target.sessionGeneration,
                BATCH_SIZE,
            )
            if (rows.isEmpty()) return Result.success()
            for (entry in rows) {
                when (process(entry)) {
                    MutationResult.Applied, MutationResult.Failed -> Unit
                    MutationResult.Retry -> return Result.retry()
                }
            }
        }
        return Result.retry()
    }

    private suspend fun process(entry: LibraryMutationEntity): MutationResult = try {
        remote.reconcile(entry)
        dao.deleteIfCurrent(entry.serverId, entry.accountId, entry.mutationKey, entry.mutationVersion)
        MutationResult.Applied
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Throwable) {
        val retry = shouldRetryLibraryMutation(failure)
        val snapshot = syncFailureSnapshot(failure)
        dao.recordAttempt(
            serverId = entry.serverId,
            accountId = entry.accountId,
            mutationKey = entry.mutationKey,
            version = entry.mutationVersion,
            state = if (retry) MUTATION_PENDING else MUTATION_FAILED,
            attemptedAt = System.currentTimeMillis(),
            code = snapshot.code,
            status = snapshot.statusCode,
            requestId = snapshot.requestId,
        )
        if (retry) MutationResult.Retry else MutationResult.Failed
    }

    private fun inputTarget(): MutationTarget? {
        val serverId = inputData.getString(LibraryMutationWorkContract.SERVER_ID) ?: return null
        val accountId = inputData.getString(LibraryMutationWorkContract.ACCOUNT_ID) ?: return null
        val baseUrl = inputData.getString(LibraryMutationWorkContract.BASE_URL) ?: return null
        val generation = inputData.getLong(LibraryMutationWorkContract.SESSION_GENERATION, -1L)
        if (generation < 0L) return null
        return MutationTarget(AccountScope(serverId, accountId), baseUrl, generation)
    }

    private suspend fun isCurrent(target: MutationTarget): Boolean {
        val accountGeneration = accountDao.get(
            target.scope.serverId,
            target.scope.accountId,
        )?.sessionGeneration
        val currentBaseUrl = serverRepository.getServer(target.scope.serverId)?.baseUrl
        return accountGeneration == target.sessionGeneration && currentBaseUrl == target.baseUrl
    }

    private data class MutationTarget(
        val scope: AccountScope,
        val baseUrl: String,
        val sessionGeneration: Long,
    )

    private enum class MutationResult { Applied, Retry, Failed }

    private companion object {
        const val BATCH_SIZE = 20
        const val MAX_BATCHES = 5
    }
}

internal fun shouldRetryLibraryMutation(failure: Throwable): Boolean {
    val status = (failure as? dev.typetype.android.core.error.CodedFailure)?.statusCode
    return failure is IOException || status == 408 || status == 429 || status != null && status >= 500
}
