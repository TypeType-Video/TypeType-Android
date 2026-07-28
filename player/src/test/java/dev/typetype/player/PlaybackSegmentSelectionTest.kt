package dev.typetype.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackSegmentSelectionTest {
    @Test
    fun `initial load selects segment covering requested position`() {
        val earlier = segment("1", startUs = 0L, durationUs = 5_000_000L)
        val covering = segment("2", startUs = 5_000_000L, durationUs = 5_000_000L)

        val selected = selectPlaybackSegment(
            segments = listOf(earlier, covering),
            targetUs = 7_000_000L,
            queuedUrls = emptySet(),
        )

        assertEquals(covering, selected)
    }

    @Test
    fun `refresh never queues the same segment twice`() {
        val current = segment("7", startUs = 30_000_000L, durationUs = 5_020_000L)
        val next = segment("8", startUs = 35_020_000L, durationUs = 5_000_000L)

        val selected = selectPlaybackSegment(
            segments = listOf(current, next),
            targetUs = 35_000_000L,
            queuedUrls = setOf(current.url),
        )

        assertEquals(next, selected)
    }

    @Test
    fun `refresh waits when server only returns queued segment`() {
        val current = segment("7", startUs = 30_000_000L, durationUs = 5_020_000L)

        val selected = selectPlaybackSegment(
            segments = listOf(current),
            targetUs = 35_000_000L,
            queuedUrls = setOf(current.url),
        )

        assertNull(selected)
    }

    private fun segment(id: String, startUs: Long, durationUs: Long) =
        PlaybackSegment(
            url = "https://example.test/segment/$id",
            startPositionUs = startUs,
            durationUs = durationUs,
        )
}
