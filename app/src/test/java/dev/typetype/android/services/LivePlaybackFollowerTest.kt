package dev.typetype.android.services

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LivePlaybackFollowerTest {
    @Test
    fun `a followed live stream catches up only after meaningful drift`() {
        val follower = LivePlaybackFollower()
        follower.transition("live")
        follower.initialize("live", positionMs = 80_000L, targetMs = 80_000L)

        assertNull(
            follower.nextTarget("live", 61_000L, 80_000L, true, false, 1_000L),
        )
        assertEquals(
            90_000L,
            follower.nextTarget("live", 60_000L, 90_000L, true, false, 2_000L),
        )
        assertNull(
            follower.nextTarget("live", 60_000L, 95_000L, true, false, 3_000L),
        )
    }

    @Test
    fun `a user seek into dvr disables automatic catch up`() {
        val follower = LivePlaybackFollower()
        follower.transition("live")
        follower.initialize("live", positionMs = 80_000L, targetMs = 80_000L)
        follower.observeSeek("live", positionMs = 20_000L, targetMs = 80_000L)

        assertNull(
            follower.nextTarget("live", 20_000L, 90_000L, true, false, 20_000L),
        )
    }

    @Test
    fun `seeking back to the edge resumes following`() {
        val follower = LivePlaybackFollower()
        follower.transition("live")
        follower.initialize("live", positionMs = 80_000L, targetMs = 80_000L)
        follower.observeSeek("live", positionMs = 20_000L, targetMs = 80_000L)
        follower.observeSeek("live", positionMs = 86_000L, targetMs = 90_000L)

        assertEquals(
            120_000L,
            follower.nextTarget("live", 86_000L, 120_000L, true, false, 20_000L),
        )
    }
}
