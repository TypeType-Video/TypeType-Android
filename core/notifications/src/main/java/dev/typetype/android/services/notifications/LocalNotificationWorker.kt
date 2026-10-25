package dev.typetype.android.services.notifications

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.typetype.android.data.account.ActiveAccountScope
import dev.typetype.android.data.diagnostics.PushDiagnosticEvent
import dev.typetype.android.data.diagnostics.PushDiagnosticRecorder
import dev.typetype.android.services.push.NotificationPostResult
import dev.typetype.android.services.push.PushNotifier
import kotlinx.coroutines.flow.first

@HiltWorker
class LocalNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val poller: LocalNotificationPoller,
    private val notifier: PushNotifier,
    private val activeAccountScope: ActiveAccountScope,
    private val pushDiagnostics: PushDiagnosticRecorder,
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val blockedReason = notifier.blockedReason()
        if (blockedReason != null) {
            val scope = activeAccountScope.observe().first()
            scope?.let {
                pushDiagnostics.record(
                    it,
                    PushDiagnosticEvent.LocalNotificationSuppressed,
                    failureCode = blockedReason.failureCode(),
                )
            }
            return Result.success()
        }
        return poller.poll().fold(
            onSuccess = { Result.success() },
            onFailure = { failure ->
                if (failure is IllegalStateException) Result.success() else Result.retry()
            },
        )
    }

    private fun NotificationPostResult.failureCode(): String = when (this) {
        NotificationPostResult.NotificationsDisabled -> "notifications_disabled"
        NotificationPostResult.ChannelBlocked -> "channel_blocked"
        NotificationPostResult.PermissionDenied -> "permission_denied"
        NotificationPostResult.Failed -> "notification_manager_unavailable"
        NotificationPostResult.Posted -> "notification_available"
    }
}
