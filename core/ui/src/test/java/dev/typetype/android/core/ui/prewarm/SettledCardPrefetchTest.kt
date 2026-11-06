package dev.typetype.android.core.ui.prewarm

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Test

class SettledCardPrefetchTest {
    private var clock = 0L
    private val ticks = Channel<Unit>()
    private val prewarm = RecordingPrewarm()

    @Test
    fun `prefetches the two most visible cards once the list settles`() = runBlocking {
        val prefetch = prefetch()
        prefetch.place("a", request("a"), visibleFraction = 0.6f, top = 0f)
        prefetch.place("b", request("b"), visibleFraction = 1f, top = 300f)
        prefetch.place("c", request("c"), visibleFraction = 1f, top = 600f)

        settle()

        assertEquals(listOf(listOf(request("b"), request("c"))), prewarm.batches)
    }

    @Test
    fun `waits for the list to stay still`() = runBlocking {
        val prefetch = prefetch()
        prefetch.place("a", request("a"), visibleFraction = 1f, top = 0f)
        yield()
        clock += 400L
        prefetch.place("a", request("a"), visibleFraction = 1f, top = -40f)
        clock += 400L
        ticks.send(Unit)
        yield()

        assertEquals(emptyList<List<PrewarmRequest>>(), prewarm.batches)
        settle()
        assertEquals(listOf(listOf(request("a"))), prewarm.batches)
    }

    @Test
    fun `moving again cancels the running prefetch`() = runBlocking {
        val prefetch = prefetch()
        prefetch.place("a", request("a"), visibleFraction = 1f, top = 0f)
        settle()

        prefetch.place("a", request("a"), visibleFraction = 1f, top = -80f)

        assertEquals(1, prewarm.cancellations)
        settle()
    }

    @Test
    fun `skips cards that are mostly off screen`() = runBlocking {
        val prefetch = prefetch()
        prefetch.place("a", request("a"), visibleFraction = 0.3f, top = 900f)

        settle()

        assertEquals(emptyList<List<PrewarmRequest>>(), prewarm.batches)
    }

    private fun kotlinx.coroutines.CoroutineScope.prefetch() =
        SettledCardPrefetch(this, prewarm, now = { clock }, wait = { ticks.receive() })

    private suspend fun settle() {
        yield()
        clock += 600L
        ticks.send(Unit)
        yield()
    }

    private fun request(id: String) = PrewarmRequest("https://www.youtube.com/watch?v=$id", knownLive = false)

    private class RecordingPrewarm : PlaybackPrewarm {
        val batches = mutableListOf<List<PrewarmRequest>>()
        var cancellations = 0

        override fun prefetchStreams(requests: List<PrewarmRequest>) {
            batches += requests
        }

        override fun cancelStreamPrefetch() {
            cancellations++
        }

        override fun pressSession(request: PrewarmRequest): PressedSession = object : PressedSession {
            override fun confirm() = Unit

            override fun cancel() = Unit
        }
    }
}
