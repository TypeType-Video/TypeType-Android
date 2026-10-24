package dev.typetype.android.data.push

import dev.typetype.android.data.account.AccountScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PushInstanceNamesTest {

    @Test
    fun `round trips scope through instance name`() {
        val scope = AccountScope(serverId = "server-1", accountId = "user-1")

        assertEquals(scope, scopeFromInstanceName(pushInstanceName(scope)))
    }

    @Test
    fun `rejects malformed instance names`() {
        assertNull(scopeFromInstanceName("no-separator"))
        assertNull(scopeFromInstanceName(":user"))
        assertNull(scopeFromInstanceName("server:"))
    }
}
