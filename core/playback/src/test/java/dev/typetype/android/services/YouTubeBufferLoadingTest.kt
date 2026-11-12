package dev.typetype.android.services

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YouTubeBufferLoadingTest {
    @Test
    fun longerTargetsContinuePastThirtySecondsAndRefillBeforeEmpty() {
        assertTrue(load(30, false))
        assertTrue(load(79, false))
        assertFalse(load(90, false))
        assertTrue(load(90, true))
        assertFalse(load(120, true))
    }

    @Test
    fun memoryBudgetAlwaysWinsEvenWhenTheDurationTargetIsNotReached() {
        for (memoryClass in listOf(96, 128, 256)) {
            val bytes = playbackBufferPolicy(memoryClass, false).targetBufferBytes
            assertFalse(shouldLoadYouTubeBuffer(1_000_000, 120_000_000, bytes, bytes, true))
            assertTrue(shouldLoadYouTubeBuffer(1_000_000, 120_000_000, bytes - 1, bytes, false))
        }
    }

    private fun load(seconds: Int, wasLoading: Boolean) = shouldLoadYouTubeBuffer(
        bufferedDurationUs = seconds * 1_000_000L,
        goalUs = 120_000_000L,
        allocatedBytes = 0,
        budgetBytes = 32 * 1024 * 1024,
        wasLoading = wasLoading,
    )
}
