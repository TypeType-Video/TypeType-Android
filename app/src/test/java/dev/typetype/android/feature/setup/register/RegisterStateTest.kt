package dev.typetype.android.feature.setup.register

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RegisterStateTest {
    @Test
    fun bootstrapOpensLocalRegistrationWhenPublicRegistrationIsClosed() {
        val state = RegisterState(
            isLoading = false,
            allowRegistration = false,
            bootstrapAvailable = true,
            localLoginEnabled = true,
        )

        assertTrue(state.isRegistrationOpen)
        assertFalse(state.isClosed)
    }

    @Test
    fun localPolicyStillAppliesDuringBootstrap() {
        val state = RegisterState(
            isLoading = false,
            allowRegistration = true,
            bootstrapAvailable = true,
            localLoginEnabled = false,
        )

        assertFalse(state.isRegistrationOpen)
        assertFalse(state.isClosed)
    }

    @Test
    fun closedPolicyIsVisibleAfterStatusLoads() {
        val state = RegisterState(isLoading = false)

        assertFalse(state.isRegistrationOpen)
        assertTrue(state.isClosed)
    }
}
