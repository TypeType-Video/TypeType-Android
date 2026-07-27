package dev.typetype.android.data.library.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.typetype.android.data.account.AccountScope
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProgressSyncScheduler @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val workManager = WorkManager.getInstance(context)

    fun enqueue(scope: AccountScope, baseUrl: String, generation: Long) {
        val request = OneTimeWorkRequestBuilder<ProgressSyncWorker>()
            .setInputData(ProgressSyncWorkContract.input(scope.serverId, scope.accountId, baseUrl, generation))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, MIN_BACKOFF_SECONDS, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniqueWork(workName(scope, generation), ExistingWorkPolicy.KEEP, request)
    }

    private fun workName(scope: AccountScope, generation: Long): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("${scope.serverId}\u0000${scope.accountId}\u0000$generation".toByteArray())
            .take(12)
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        return "typetype-progress-$digest"
    }

    private companion object {
        const val MIN_BACKOFF_SECONDS = 10L
    }
}
