package dev.typetype.android.services

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderAudioOnlyRenewalGateTest {
    @Test
    fun `allows one renewal while a request is in flight`() {
        val gate = ProviderAudioOnlyRenewalGate(minimumIntervalMs = 1_000L)

        assertTrue(gate.begin("video", nowMs = 100L))
        assertFalse(gate.begin("video", nowMs = 200L))
    }

    @Test
    fun `requires a stable interval before another renewal`() {
        val gate = ProviderAudioOnlyRenewalGate(minimumIntervalMs = 1_000L)

        assertTrue(gate.begin("video", nowMs = 100L))
        gate.finish()

        assertFalse(gate.begin("video", nowMs = 1_099L))
        assertTrue(gate.begin("video", nowMs = 1_100L))
    }

    @Test
    fun `a media transition resets the renewal budget`() {
        val gate = ProviderAudioOnlyRenewalGate(minimumIntervalMs = 1_000L)

        assertTrue(gate.begin("first", nowMs = 100L))

        assertTrue(gate.begin("second", nowMs = 200L))
    }
}
