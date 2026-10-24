package dev.typetype.android.services

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackRecoveryPolicyTest {
    private val policy = PlaybackRecoveryPolicy()

    @Test
    fun attemptsStartAtOneSecondAndDoubleUpToEight() {
        assertEquals(listOf(1_000L, 2_000L, 4_000L, 8_000L, 8_000L), (1..5).map(policy::delayMs))
    }

    @Test
    fun theUserSeesAnErrorOnlyAfterTwoMinutes() {
        assertFalse(policy.isExhausted(119_999))
        assertTrue(policy.isExhausted(120_000))
    }
}
