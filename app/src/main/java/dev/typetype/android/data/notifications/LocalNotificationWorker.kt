package dev.typetype.android.data.notifications

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.typetype.android.services.push.PushNotifier

@HiltWorker
class LocalNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val poller: LocalNotificationPoller,
    private val notifier: PushNotifier,
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        if (!notifier.canNotify()) return Result.success()
        return poller.poll().fold(
            onSuccess = { Result.success() },
            onFailure = { failure ->
                if (failure is IllegalStateException) Result.success() else Result.retry()
            },
        )
    }
}
