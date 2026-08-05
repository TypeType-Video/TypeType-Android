package dev.typetype.android.feature.player.components

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutomaticSabrPlaybackRecoveryGateTest {
    @Test
    fun `claims only one automatic retry for active media`() {
        val gate = AutomaticSabrPlaybackRecoveryGate()

        assertTrue(gate.claim("video"))
        assertFalse(gate.claim("video"))
    }

    @Test
    fun `stable playback rearms active media`() {
        val gate = AutomaticSabrPlaybackRecoveryGate()

        assertTrue(gate.claim("video"))
        gate.rearm("video")

        assertTrue(gate.claim("video"))
    }

    @Test
    fun `media transition starts a separate recovery episode`() {
        val gate = AutomaticSabrPlaybackRecoveryGate()

        assertTrue(gate.claim("first"))
        assertTrue(gate.claim("second"))
        assertFalse(gate.claim(null))
    }

    @Test
    fun `unrelated stable playback cannot rearm current media`() {
        val gate = AutomaticSabrPlaybackRecoveryGate()

        assertTrue(gate.claim("video"))
        gate.rearm("other")

        assertFalse(gate.claim("video"))
    }
}
