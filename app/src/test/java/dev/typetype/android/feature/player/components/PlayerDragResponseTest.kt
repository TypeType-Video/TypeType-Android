package dev.typetype.android.feature.player.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerDragResponseTest {
    @Test fun `seek scales with viewport rather than pixel density`() {
        assertEquals(
            proportionalSeekTarget(100_000, 100f, 1000f, 600_000),
            proportionalSeekTarget(100_000, 200f, 2000f, 600_000),
        )
    }

    @Test fun `larger movement accelerates and reversing returns to anchor`() {
        val small = proportionalSeekTarget(100_000, 100f, 1000f, 600_000) - 100_000
        val large = proportionalSeekTarget(100_000, 200f, 1000f, 600_000) - 100_000
        assertTrue(large > small * 2)
        assertEquals(100_000L, proportionalSeekTarget(100_000, 0f, 1000f, 600_000))
        assertEquals(0L, proportionalSeekTarget(0, -500f, 1000f, 600_000))
        assertEquals(600_000L, proportionalSeekTarget(600_000, 500f, 1000f, 600_000))
    }

    @Test fun `invalid duration leaves playback unchanged`() {
        assertEquals(15_000L, proportionalSeekTarget(15_000, 100f, 1000f, -1))
    }

    @Test fun `hold starts at two and ignores jitter`() {
        val steps = HoldSpeedSteps(24f)
        assertEquals(2f, steps.update(-23f))
        assertEquals(2.25f, steps.update(-24f))
        assertEquals(2.25f, steps.update(-22f))
        assertEquals(2f, steps.update(0f))
    }

    @Test fun `hold limits speed and responds immediately when reversing at bounds`() {
        val steps = HoldSpeedSteps(24f)
        assertEquals(4f, steps.update(-480f))
        assertEquals(3.75f, steps.update(-456f))
        assertEquals(0.25f, steps.update(480f))
        assertEquals(0.5f, steps.update(456f))
    }
}
