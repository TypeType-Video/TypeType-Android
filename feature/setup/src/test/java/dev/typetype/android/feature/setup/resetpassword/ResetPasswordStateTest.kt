package dev.typetype.android.feature.setup.resetpassword

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResetPasswordStateTest {
    @Test
    fun requiresBothTokenAndPassword() {
        assertFalse(ResetPasswordState().canSubmit)
        assertFalse(ResetPasswordState(resetToken = "token").canSubmit)
        assertFalse(ResetPasswordState(newPassword = "secret").canSubmit)
    }

    @Test
    fun acceptsCompleteInputOnlyWhenIdle() {
        assertTrue(
            ResetPasswordState(
                resetToken = "token",
                newPassword = "secret",
            ).canSubmit,
        )
        assertFalse(
            ResetPasswordState(
                resetToken = "token",
                newPassword = "secret",
                isSubmitting = true,
            ).canSubmit,
        )
    }
}
