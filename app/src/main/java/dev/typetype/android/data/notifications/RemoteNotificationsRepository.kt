package dev.typetype.android.data.notifications

import dev.typetype.android.data.account.AccountDao
import dev.typetype.android.data.account.AccountEntity
import dev.typetype.android.data.account.AccountScope
import dev.typetype.android.data.account.AccountScopedValue
import dev.typetype.android.data.account.ActiveAccountScope
import dev.typetype.android.data.network.TypeTypeApiHolder
import dev.typetype.android.data.network.dto.toDomain
import dev.typetype.android.data.network.requireSuccessfulResponse
import dev.typetype.android.domain.notifications.NotificationBadge
import dev.typetype.android.domain.notifications.NotificationsPage
import dev.typetype.android.domain.notifications.NotificationsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

@Singleton
class RemoteNotificationsRepository @Inject constructor(
    private val apiHolder: TypeTypeApiHolder,
    private val activeAccountScope: ActiveAccountScope,
    private val accountDao: AccountDao,
) : NotificationsRepository {
    private val unreadState = MutableStateFlow<AccountScopedValue<Int>?>(null)

    override fun observeBadge(): Flow<NotificationBadge> = combine(
        activeAccountScope.observe(),
        accountDao.observeAll(),
        unreadState,
        ::notificationBadge,
    )

    override suspend fun refreshUnreadCount(): Result<Unit> = runCatching {
        val scope = eligibleScope() ?: return@runCatching
        val response = withContext(Dispatchers.IO) {
            apiHolder.require(scope).unreadNotificationsCount()
        }
        response.requireSuccessfulResponse()
        val body = response.body() ?: error("The instance returned an empty notification count")
        activeAccountScope.verify(scope)
        unreadState.value = AccountScopedValue(scope, body.unreadCount.coerceAtLeast(0))
    }

    override suspend fun page(page: Int): Result<NotificationsPage> = runCatching {
        val scope = requireNotNull(eligibleScope()) {
            "Notifications are unavailable for guest accounts"
        }
        val response = withContext(Dispatchers.IO) {
            apiHolder.require(scope).notifications(page.coerceAtLeast(0), PAGE_SIZE)
        }
        response.requireSuccessfulResponse()
        val result = response.body()?.toDomain()
            ?: error("The instance returned an empty notifications page")
        activeAccountScope.verify(scope)
        unreadState.value = AccountScopedValue(scope, result.unreadCount)
        result
    }

    override suspend fun markAllRead(): Result<Unit> = runCatching {
        val scope = requireNotNull(eligibleScope()) {
            "Notifications are unavailable for guest accounts"
        }
        val response = withContext(Dispatchers.IO) {
            apiHolder.require(scope).markAllNotificationsRead()
        }
        response.requireSuccessfulResponse()
        val body = response.body() ?: error("The instance returned an empty read state")
        activeAccountScope.verify(scope)
        unreadState.value = AccountScopedValue(scope, body.unreadCount.coerceAtLeast(0))
    }

    private suspend fun eligibleScope(): AccountScope? {
        val scope = activeAccountScope.require()
        val account = accountDao.get(scope.serverId, scope.accountId)
        return scope.takeIf { account != null && !account.isGuest }
    }

    private companion object {
        const val PAGE_SIZE = 20
    }
}

internal fun notificationBadge(
    scope: AccountScope?,
    accounts: List<AccountEntity>,
    cached: AccountScopedValue<Int>?,
): NotificationBadge {
    val account = accounts.firstOrNull {
        scope != null && it.serverId == scope.serverId && it.accountId == scope.accountId
    }
    return NotificationBadge(
        isAvailable = account != null && !account.isGuest,
        unreadCount = cached?.value
            ?.takeIf { cached.scope == scope && account?.isGuest == false }
            ?: 0,
    )
}
