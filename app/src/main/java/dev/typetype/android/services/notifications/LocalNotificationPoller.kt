package dev.typetype.android.services.notifications

import dev.typetype.android.data.account.AccountDao
import dev.typetype.android.data.account.AccountScope
import dev.typetype.android.data.account.ActiveAccountScope
import dev.typetype.android.data.diagnostics.PushDiagnosticEvent
import dev.typetype.android.data.diagnostics.PushDiagnosticRecorder
import dev.typetype.android.domain.notifications.NotificationsRepository
import dev.typetype.android.domain.push.PushRepository
import dev.typetype.android.services.push.NotificationPostResult
import dev.typetype.android.services.push.PushNotifier
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

@Singleton
class LocalNotificationPoller @Inject constructor(
    private val activeAccountScope: ActiveAccountScope,
    private val accountDao: AccountDao,
    private val notifications: NotificationsRepository,
    private val pushRepository: PushRepository,
    private val seenStore: LocalNotificationSeenStore,
    private val notifier: PushNotifier,
    private val pushDiagnostics: PushDiagnosticRecorder,
) {
    suspend fun poll(): Result<Int> = try {
        Result.success(pollOnce())
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Throwable) {
        Result.failure(failure)
    }

    private suspend fun pollOnce(): Int {
        val scope = eligibleScope()
        val enabledChannels = pushRepository.channelPreferences().getOrThrow()
        val page = notifications.page(0).getOrThrow()
        pushDiagnostics.record(scope, PushDiagnosticEvent.LocalPollChecked)
        val state = seenStore.state(scope)
        val decision = decideLocalNotifications(
            items = page.items,
            knownKeys = state.keys.toHashSet(),
            seeded = state.seeded,
            enabledChannels = enabledChannels,
        )
        if (decision.keysToRecord.isEmpty() && decision.notified.isEmpty()) return 0
        val unpostedKeys = mutableSetOf<String>()
        var postedCount = 0
        decision.notified.forEach { item ->
            when (notifier.notifySubscription(item)) {
                NotificationPostResult.Posted -> {
                    postedCount += 1
                    pushDiagnostics.record(scope, PushDiagnosticEvent.LocalNotificationPosted)
                }
                NotificationPostResult.NotificationsDisabled -> {
                    unpostedKeys += item.localNotificationKey()
                    pushDiagnostics.record(
                        scope,
                        PushDiagnosticEvent.LocalNotificationSuppressed,
                        failureCode = "notifications_disabled",
                    )
                }
                NotificationPostResult.ChannelBlocked -> {
                    unpostedKeys += item.localNotificationKey()
                    pushDiagnostics.record(
                        scope,
                        PushDiagnosticEvent.LocalNotificationSuppressed,
                        failureCode = "channel_blocked",
                    )
                }
                NotificationPostResult.PermissionDenied -> {
                    unpostedKeys += item.localNotificationKey()
                    pushDiagnostics.record(
                        scope,
                        PushDiagnosticEvent.LocalNotificationSuppressed,
                        failureCode = "permission_denied",
                    )
                }
                NotificationPostResult.Failed -> {
                    unpostedKeys += item.localNotificationKey()
                    pushDiagnostics.record(
                        scope,
                        PushDiagnosticEvent.LocalNotificationFailed,
                        failureCode = "notification_manager_unavailable",
                    )
                }
            }
        }
        seenStore.record(
            scope,
            decision.keysToPersist(unpostedKeys),
            seeded = true,
        )
        return postedCount
    }

    private suspend fun eligibleScope(): AccountScope {
        val scope = activeAccountScope.require()
        val account = accountDao.get(scope.serverId, scope.accountId)
        check(account != null && !account.isGuest) {
            "Local notifications are unavailable for guest accounts"
        }
        return scope
    }
}
