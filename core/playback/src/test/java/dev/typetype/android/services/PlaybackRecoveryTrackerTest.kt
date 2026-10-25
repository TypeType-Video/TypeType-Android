package dev.typetype.android.services

import dev.typetype.android.data.network.PlaybackNetworkState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackRecoveryTrackerTest {
    private var nowMs = 0L
    private val tracker = PlaybackRecoveryTracker(clock = { nowMs })
    private val online = PlaybackNetworkState(isAvailable = true, generation = 1)
    private val offline = PlaybackNetworkState(isAvailable = false, generation = 2)

    @Test
    fun failuresAreRetriedWithGrowingPausesUntilTheWindowCloses() {
        assertEquals(PlaybackRecoveryDecision.RetryAfter(1_000), tracker.failed("video", online))
        assertEquals(PlaybackRecoveryDecision.RetryAfter(2_000), tracker.failed("video", online))
        nowMs = 120_000
        assertEquals(PlaybackRecoveryDecision.GiveUp, tracker.failed("video", online))
    }

    @Test
    fun shortSuccessesDoNotReopenTheWindow() {
        tracker.failed("video", online)
        nowMs = 60_000
        tracker.stablePlayback(10_000)
        nowMs = 121_000
        assertEquals(PlaybackRecoveryDecision.GiveUp, tracker.failed("video", online))
    }

    @Test
    fun cleanPlaybackForThirtySecondsStartsAFreshWindow() {
        tracker.failed("video", online)
        nowMs = 100_000
        tracker.stablePlayback(30_000)
        nowMs = 200_000
        assertEquals(PlaybackRecoveryDecision.RetryAfter(1_000), tracker.failed("video", online))
    }

    @Test
    fun withoutANetworkItWaitsAndRetriesAsSoonAsOneReturns() {
        assertEquals(PlaybackRecoveryDecision.WaitForNetwork, tracker.failed("video", offline))
        assertTrue(tracker.networkReturned())
        assertFalse(tracker.networkReturned())
    }

    @Test
    fun anotherVideoGetsItsOwnWindow() {
        tracker.failed("first", online)
        nowMs = 150_000
        assertEquals(PlaybackRecoveryDecision.RetryAfter(1_000), tracker.failed("second", online))
    }
}
