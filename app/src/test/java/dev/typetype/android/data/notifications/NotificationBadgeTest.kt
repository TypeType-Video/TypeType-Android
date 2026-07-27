package dev.typetype.android.data.notifications

import dev.typetype.android.data.account.AccountEntity
import dev.typetype.android.data.account.AccountScope
import dev.typetype.android.data.account.AccountScopedValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationBadgeTest {
    @Test
    fun exposesOnlyTheActiveSignedInAccountCount() {
        val active = AccountScope("server", "account")
        val badge = notificationBadge(
            scope = active,
            accounts = listOf(account("server", "account", isGuest = false)),
            cached = AccountScopedValue(active, 7),
        )

        assertTrue(badge.isAvailable)
        assertEquals(7, badge.unreadCount)
    }

    @Test
    fun neverLeaksAnotherAccountsCount() {
        val active = AccountScope("server", "second")
        val badge = notificationBadge(
            scope = active,
            accounts = listOf(account("server", "second", isGuest = false)),
            cached = AccountScopedValue(AccountScope("server", "first"), 12),
        )

        assertTrue(badge.isAvailable)
        assertEquals(0, badge.unreadCount)
    }

    @Test
    fun hidesNotificationsForGuestAccounts() {
        val active = AccountScope("server", "guest:account")
        val badge = notificationBadge(
            scope = active,
            accounts = listOf(account("server", "guest:account", isGuest = true)),
            cached = AccountScopedValue(active, 4),
        )

        assertFalse(badge.isAvailable)
        assertEquals(0, badge.unreadCount)
    }

    private fun account(serverId: String, accountId: String, isGuest: Boolean) = AccountEntity(
        serverId = serverId,
        accountId = accountId,
        publicUsername = null,
        role = null,
        avatarUrl = null,
        avatarType = null,
        avatarCode = null,
        isGuest = isGuest,
        lastUsedAt = 0L,
        sessionGeneration = 0L,
    )
}
