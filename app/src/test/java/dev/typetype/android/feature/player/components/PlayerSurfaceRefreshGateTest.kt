package dev.typetype.android.feature.player.components

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerSurfaceRefreshGateTest {
    @Test
    fun `background cycle without screen off does not request refresh`() {
        val gate = PlayerSurfaceRefreshGate()

        assertFalse(gate.consumeScreenOff())
        assertFalse(gate.consumeScreenOff())
    }

    @Test
    fun `screen off requests refresh once`() {
        val gate = PlayerSurfaceRefreshGate()

        gate.markScreenOff()
        assertTrue(gate.consumeScreenOff())
        assertFalse(gate.consumeScreenOff())
    }

    @Test
    fun `each screen off cycle permits one surface recreation`() {
        val gate = PlayerSurfaceRefreshGate()

        repeat(1_000) {
            gate.markScreenOff()
            assertTrue(gate.consumeScreenOff())
            assertFalse(gate.consumeScreenOff())
        }
    }
}
