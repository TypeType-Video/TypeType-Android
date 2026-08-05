package dev.typetype.android.feature.player.components

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerSurfaceRefreshGateTest {
    @Test
    fun `initial lifecycle events do not recreate the surface`() {
        val gate = PlayerSurfaceRefreshGate()

        assertFalse(gate.refresh())
        assertFalse(gate.refresh())
    }

    @Test
    fun `multiple wake signals recreate the surface once`() {
        val gate = PlayerSurfaceRefreshGate()

        gate.invalidate()
        assertTrue(gate.refresh())
        assertFalse(gate.refresh())
    }

    @Test
    fun `each inactive cycle permits one surface recreation`() {
        val gate = PlayerSurfaceRefreshGate()

        repeat(1_000) {
            gate.invalidate()
            gate.invalidate()
            assertTrue(gate.refresh())
            assertFalse(gate.refresh())
        }
    }
}
