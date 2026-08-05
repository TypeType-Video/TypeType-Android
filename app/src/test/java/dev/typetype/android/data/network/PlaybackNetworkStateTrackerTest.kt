package dev.typetype.android.data.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackNetworkStateTrackerTest {
    @Test
    fun `unvalidated self hosted route remains available`() {
        val tracker = tracker(route(validated = false))

        assertTrue(tracker.state.isAvailable)
        assertNull(tracker.update(route(validated = false)))
    }

    @Test
    fun `validation recovery signals a new generation`() {
        val tracker = tracker(route(validated = false))

        val recovered = tracker.update(route(validated = true))

        assertEquals(PlaybackNetworkState(true, 1L), recovered)
        assertEquals(recovered, tracker.state)
    }

    @Test
    fun `validation loss waits for recovery before signaling`() {
        val tracker = tracker(route(validated = true))

        assertNull(tracker.update(route(validated = false)))
        assertEquals(PlaybackNetworkState(true, 1L), tracker.update(route(validated = true)))
    }

    @Test
    fun `suspension is unavailable until the same route recovers`() {
        val tracker = tracker(route(suspended = false))

        val suspended = tracker.update(route(suspended = true))
        val recovered = tracker.update(route(suspended = false))

        assertEquals(PlaybackNetworkState(false, 1L), suspended)
        assertEquals(PlaybackNetworkState(true, 2L), recovered)
    }

    @Test
    fun `blocked route reports loss and recovery`() {
        val tracker = tracker(route())

        val blocked = tracker.update(route(blocked = true))
        val unblocked = tracker.update(route(blocked = false))

        assertFalse(blocked?.isAvailable ?: true)
        assertTrue(unblocked?.isAvailable ?: false)
        assertEquals(2L, tracker.state.generation)
    }

    @Test
    fun `lost route and replacement route each advance generation`() {
        val tracker = tracker(route(identity = "mobile"))

        val lost = tracker.update(route(identity = null))
        val replacement = tracker.update(route(identity = "wifi", validated = false))

        assertEquals(PlaybackNetworkState(false, 1L), lost)
        assertEquals(PlaybackNetworkState(true, 2L), replacement)
    }

    @Test
    fun `stale capability noise does not consume generations`() {
        val tracker = tracker(route())

        repeat(10_000) {
            assertNull(tracker.update(route()))
        }

        assertEquals(0L, tracker.state.generation)
    }

    @Test
    fun `link property signal advances generation without changing availability`() {
        val tracker = tracker(route())

        val changed = tracker.update(route(), routeSignaled = true)

        assertEquals(PlaybackNetworkState(true, 1L), changed)
    }

    private fun tracker(route: PlaybackNetworkRoute) = PlaybackNetworkStateTracker(route)

    private fun route(
        identity: Any? = "wifi",
        blocked: Boolean = false,
        validated: Boolean? = true,
        suspended: Boolean? = false,
    ) = PlaybackNetworkRoute(
        identity = identity,
        isBlocked = blocked,
        isValidated = validated,
        isSuspended = suspended,
    )
}
