package dev.typetype.android.feature.player.components

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerDragResponseTest {
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
