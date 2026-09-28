package dev.typetype.android.data.notifications

import dev.typetype.android.data.account.AccountDao
import dev.typetype.android.data.account.AccountScope
import dev.typetype.android.data.account.ActiveAccountScope
import dev.typetype.android.domain.notifications.NotificationsRepository
import dev.typetype.android.domain.push.PushRepository
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
        val state = seenStore.state(scope)
        val decision = decideLocalNotifications(
            items = page.items,
            knownKeys = state.keys.toHashSet(),
            seeded = state.seeded,
            enabledChannels = enabledChannels,
        )
        if (decision.keysToRecord.isEmpty() && decision.notified.isEmpty()) return 0
        seenStore.record(scope, decision.keysToRecord, seeded = true)
        decision.notified.forEach(notifier::notifySubscription)
        return decision.notified.size
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
