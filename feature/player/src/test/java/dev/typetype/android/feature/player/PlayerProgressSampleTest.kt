package dev.typetype.android.feature.player

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlayerProgressSampleTest {
    private val old = MediaItem.Builder().setMediaId("https://www.youtube.com/watch?v=old").build()
    private val next = MediaItem.Builder().setMediaId("https://www.youtube.com/watch?v=next").build()

    @Test
    fun `a replaced video saves its own last position under its own url`() {
        val sample = discontinuityProgressSample(
            oldPosition = position(old, 0, 75_000L),
            newPosition = position(next, 0, 0L),
            reason = Player.DISCONTINUITY_REASON_REMOVE,
            durationMillis = 600_000L,
        )

        assertEquals(PlayerProgressSample(old.mediaId, 75_000L), sample)
    }

    @Test
    fun `a seek saves the new position of the playing video`() {
        val sample = discontinuityProgressSample(
            oldPosition = position(next, 0, 10_000L),
            newPosition = position(next, 0, 90_000L),
            reason = Player.DISCONTINUITY_REASON_SEEK,
            durationMillis = 600_000L,
        )

        assertEquals(PlayerProgressSample(next.mediaId, 90_000L), sample)
    }

    @Test
    fun `a video that played to its end is not saved as a resume point`() {
        val sample = discontinuityProgressSample(
            oldPosition = position(old, 0, 599_000L),
            newPosition = position(next, 1, 0L),
            reason = Player.DISCONTINUITY_REASON_AUTO_TRANSITION,
            durationMillis = 600_000L,
        )

        assertNull(sample)
    }

    @Test
    fun `too early, nearly finished or unidentified positions are skipped`() {
        assertNull(playerProgressSample(next, 2_000L, 600_000L))
        assertNull(playerProgressSample(next, 590_000L, 600_000L))
        assertNull(playerProgressSample(null, 60_000L, 600_000L))
    }

    private fun position(item: MediaItem, index: Int, positionMs: Long) = Player.PositionInfo(
        null,
        index,
        item,
        null,
        index,
        positionMs,
        positionMs,
        -1,
        -1,
    )
}
