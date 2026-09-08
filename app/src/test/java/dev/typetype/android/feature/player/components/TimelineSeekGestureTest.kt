package dev.typetype.android.feature.player.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineSeekGestureTest {
    @Test fun `full width traverses both short and twelve hour videos`() {
        for (duration in listOf(120_000L, 7_200_000L, 43_200_000L)) {
            val seek = TimelineSeekGesture(0, duration, 1000f, 1f)
            assertEquals(duration / 4, seek.move(250f, 0f))
            assertEquals(duration, seek.move(750f, 0f))
            assertEquals(0L, seek.move(-1000f, 0f))
        }
    }

    @Test fun `same fraction has same effect at different densities`() {
        assertEquals(
            TimelineSeekGesture(0, 7_200_000, 1000f, 1f).move(250f, 0f),
            TimelineSeekGesture(0, 7_200_000, 2000f, 2f).move(500f, 0f),
        )
    }

    @Test fun `vertical precision changes do not move the destination`() {
        val seek = TimelineSeekGesture(0, 7_200_000, 1000f, 1f)
        val coarse = seek.move(250f, 0f)
        assertEquals(coarse, seek.move(0f, 48f))
        assertTrue(seek.fineSeeking)
        assertEquals(coarse, seek.move(0f, 40f))
        assertTrue(seek.fineSeeking)
        assertEquals(coarse + 1000, seek.move(10f, 48f))
        assertEquals(coarse + 1000, seek.move(0f, 32f))
        assertFalse(seek.fineSeeking)
        assertEquals(coarse + 73_000, seek.move(10f, 0f))
    }

    @Test fun `fine seeking stays precise for long media and high density`() {
        val seek = TimelineSeekGesture(1_000_000, 43_200_000, 2000f, 2f)
        assertEquals(1_001_000L, seek.move(20f, 96f))
    }

    @Test fun `edge clamp allows immediate reversal`() {
        val seek = TimelineSeekGesture(0, 120_000, 1000f, 1f)
        assertEquals(0L, seek.move(-500f, 0f))
        assertEquals(1200L, seek.move(10f, 0f))
    }

    @Test fun `invalid movement is ignored`() {
        val seek = TimelineSeekGesture(60_000, 120_000, 1000f, 1f)
        assertEquals(60_000L, seek.move(Float.NaN, 0f))
        assertEquals(60_000L, seek.move(10f, Float.POSITIVE_INFINITY))
    }
}
