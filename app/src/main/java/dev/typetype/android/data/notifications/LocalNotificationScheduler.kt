package dev.typetype.android.data.notifications

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalNotificationScheduler @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val workManager = WorkManager.getInstance(context)

    fun sync(enabled: Boolean) {
        if (!enabled) {
            workManager.cancelUniqueWork(WORK_NAME)
            return
        }
        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<LocalNotificationWorker>(INTERVAL_MINUTES, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, MIN_BACKOFF_MINUTES, TimeUnit.MINUTES)
                .build(),
        )
    }

    private companion object {
        const val WORK_NAME = "typetype-local-notifications"
        const val INTERVAL_MINUTES = 15L
        const val MIN_BACKOFF_MINUTES = 5L
    }
}
