package dev.typetype.android.data.auth

import org.junit.Assert.assertThrows
import org.junit.Test

class SessionAccountValidationTest {
    @Test
    fun acceptsTheExpectedAccountAndNewSignIns() {
        requireExpectedAccount(null, "account-b")
        requireExpectedAccount("account-a", "account-a")
    }

    @Test
    fun rejectsAnotherAccountBeforeReplacingTheSession() {
        assertThrows(SessionAccountMismatchException::class.java) {
            requireExpectedAccount("account-a", "account-b")
        }
    }
}
