package dev.typetype.android.feature.player.components

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerDragResponseTest {
    @Test fun `seek uses the same sensitivity for short and long videos`() {
        assertEquals(30_000L, swipeSeekTarget(20_000, 100f, 120_000))
        assertEquals(30_000L, swipeSeekTarget(20_000, 100f, 7_200_000))
    }

    @Test fun `seek accelerates tenfold after sixty seconds of travel`() {
        assertEquals(159_900L, swipeSeekTarget(100_000, 599f, 600_000))
        assertEquals(160_000L, swipeSeekTarget(100_000, 600f, 600_000))
        assertEquals(161_000L, swipeSeekTarget(100_000, 601f, 600_000))
        assertEquals(260_000L, swipeSeekTarget(100_000, 700f, 600_000))
        assertEquals(140_000L, swipeSeekTarget(300_000, -700f, 600_000))
    }

    @Test fun `seek clamps to media and reversing returns to the anchor`() {
        assertEquals(100_000L, swipeSeekTarget(100_000, 0f, 600_000))
        assertEquals(0L, swipeSeekTarget(0, -500f, 600_000))
        assertEquals(600_000L, swipeSeekTarget(600_000, 500f, 600_000))
    }

    @Test fun `invalid duration leaves playback unchanged`() {
        assertEquals(15_000L, swipeSeekTarget(15_000, 100f, -1))
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
