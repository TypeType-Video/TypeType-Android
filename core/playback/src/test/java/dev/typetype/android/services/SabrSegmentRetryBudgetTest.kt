package dev.typetype.android.services

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SabrSegmentRetryBudgetTest {
    @Test
    fun retryBudgetAllowsEightRetriesForOneLoadTask() {
        var nowMs = 0L
        val budget = SabrSegmentRetryBudget(elapsedRealtimeMs = { nowMs })

        for (errorCount in 1..8) {
            assertEquals(250L, budget.retryDelayMs(10L, errorCount, 250L))
            nowMs += 100L
        }
        assertNull(budget.retryDelayMs(10L, 9, 250L))
    }

    @Test
    fun retryBudgetStopsWhenServerRetryDelaysExceedTwentySeconds() {
        var nowMs = 0L
        val budget = SabrSegmentRetryBudget(elapsedRealtimeMs = { nowMs })

        assertEquals(250L, budget.retryDelayMs(10L, 1, 250L))
        nowMs = 19_800L

        assertNull(budget.retryDelayMs(10L, 2, 250L))
    }

    @Test
    fun completedLoadsDoNotShareRetryTime() {
        var nowMs = 0L
        val budget = SabrSegmentRetryBudget(elapsedRealtimeMs = { nowMs })

        assertEquals(250L, budget.retryDelayMs(10L, 1, 250L))
        nowMs = 50_000L
        budget.conclude(10L)

        assertEquals(250L, budget.retryDelayMs(11L, 1, 250L))
    }
}
