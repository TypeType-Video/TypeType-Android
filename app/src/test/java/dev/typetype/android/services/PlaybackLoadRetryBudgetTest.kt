package dev.typetype.android.services

import dev.typetype.android.data.network.PlaybackNetworkObserver
import dev.typetype.android.data.network.PlaybackNetworkState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackLoadRetryBudgetTest {
    @Test
    fun `connected transport failures use a longer bounded retry budget`() {
        val network = FakeNetworkObserver(PlaybackNetworkState(true, 0L))
        val budget = PlaybackLoadRetryBudget(network) { 0L }

        assertEquals(500L, budget.retryDelayMs(1L))
        assertEquals(1_000L, budget.retryDelayMs(1L))
        assertEquals(2_000L, budget.retryDelayMs(1L))
        repeat(5) {
            assertEquals(3_000L, budget.retryDelayMs(1L))
        }
        assertNull(budget.retryDelayMs(1L))
    }

    @Test
    fun `a new network route resets the transport budget`() {
        val network = FakeNetworkObserver(PlaybackNetworkState(true, 4L))
        val budget = PlaybackLoadRetryBudget(network) { 0L }

        repeat(8) {
            budget.retryDelayMs(9L)
        }
        network.state = PlaybackNetworkState(true, 5L)

        assertEquals(500L, budget.retryDelayMs(9L))
    }

    @Test
    fun `offline media remains retryable for the recovery window`() {
        var nowMs = 0L
        val network = FakeNetworkObserver(PlaybackNetworkState(false, 2L))
        val budget = PlaybackLoadRetryBudget(network) { nowMs }

        assertEquals(3_000L, budget.retryDelayMs(3L))
        nowMs = 599_999L
        assertEquals(3_000L, budget.retryDelayMs(3L))
        nowMs = 600_000L
        assertNull(budget.retryDelayMs(3L))
    }

    @Test
    fun `concluding a load clears its previous failures`() {
        val network = FakeNetworkObserver(PlaybackNetworkState(true, 0L))
        val budget = PlaybackLoadRetryBudget(network) { 0L }

        assertEquals(500L, budget.retryDelayMs(7L))
        assertEquals(1_000L, budget.retryDelayMs(7L))
        budget.conclude(7L)

        assertEquals(500L, budget.retryDelayMs(7L))
    }
}

private class FakeNetworkObserver(
    var state: PlaybackNetworkState,
) : PlaybackNetworkObserver {
    override fun snapshot(): PlaybackNetworkState = state

    override suspend fun awaitAvailableAfter(generation: Long, timeoutMs: Long): Boolean = false
}
