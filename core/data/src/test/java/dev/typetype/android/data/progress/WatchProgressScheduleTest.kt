package dev.typetype.android.data.progress

import org.junit.Assert.assertEquals
import org.junit.Test

class WatchProgressScheduleTest {
    private val schedule = WatchProgressSchedule(freshForMs = 60_000L, maxBatchSize = 200)
    private val list = (1..30).map { "https://www.youtube.com/watch?v=video$it" }

    @Test
    fun `a whole list costs one request`() {
        assertEquals(listOf(list), schedule.dueBatches(list + list.take(5), now = 0L))
    }

    @Test
    fun `scrolling back within a minute asks for nothing, after it asks again`() {
        schedule.dueBatches(list, now = 0L)

        assertEquals(emptyList<List<String>>(), schedule.dueBatches(list, now = 30_000L))
        assertEquals(listOf(list), schedule.dueBatches(list, now = 61_000L))
    }

    @Test
    fun `only cards not yet known are fetched and a failed batch is retried`() {
        schedule.dueBatches(list.take(10), now = 0L)

        assertEquals(listOf(list.drop(10)), schedule.dueBatches(list, now = 1_000L))
        schedule.forget(list.drop(10))
        assertEquals(listOf(list.drop(10)), schedule.dueBatches(list, now = 2_000L))
    }

    @Test
    fun `very long lists are split at the server batch limit`() {
        val long = (1..450).map { "https://www.youtube.com/watch?v=long$it" }

        assertEquals(listOf(200, 200, 50), schedule.dueBatches(long, now = 0L).map { it.size })
    }
}
