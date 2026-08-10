package dev.typetype.android.feature.settings

import dev.typetype.android.data.account.AccountScope
import dev.typetype.android.domain.account.Account
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsAccountFeaturesTest {
    private val account = Account(
        serverId = "server-a",
        id = "account-a",
        publicUsername = null,
        role = "user",
        avatarUrl = null,
        avatarType = null,
        avatarCode = null,
        isGuest = false,
        lastUsedAt = 1L,
    )

    @Test
    fun `authenticated account stays eligible without a loaded profile`() {
        val available = accountFeaturesAvailable(
            scope = AccountScope("server-a", "account-a"),
            accounts = listOf(account),
        )

        assertTrue(available)
    }

    @Test
    fun `guest and inactive accounts are not eligible`() {
        val guest = account.copy(isGuest = true)

        assertFalse(
            accountFeaturesAvailable(AccountScope("server-a", "account-a"), listOf(guest)),
        )
        assertFalse(
            accountFeaturesAvailable(AccountScope("server-b", "account-a"), listOf(account)),
        )
        assertFalse(accountFeaturesAvailable(null, listOf(account)))
    }
}
