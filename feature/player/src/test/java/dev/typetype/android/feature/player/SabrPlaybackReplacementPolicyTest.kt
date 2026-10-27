package dev.typetype.android.feature.player

import dev.typetype.android.domain.stream.StreamPlaybackContract
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SabrPlaybackReplacementPolicyTest {
    @Test
    fun `paused SABR media remains paused after session replacement`() {
        assertFalse(
            replacementPlayWhenReady(
                StreamPlaybackContract.ServerSabr,
                sameMedia = true,
                currentPlayWhenReady = false,
            ),
        )
    }

    @Test
    fun `playing SABR media continues after session replacement`() {
        assertTrue(
            replacementPlayWhenReady(
                StreamPlaybackContract.ServerSabr,
                sameMedia = true,
                currentPlayWhenReady = true,
            ),
        )
    }

    @Test
    fun `new SABR media starts playback`() {
        assertTrue(
            replacementPlayWhenReady(
                StreamPlaybackContract.ServerSabr,
                sameMedia = false,
                currentPlayWhenReady = false,
            ),
        )
    }

    @Test
    fun `restored SABR media remains paused until the user resumes`() {
        assertFalse(
            replacementPlayWhenReady(
                StreamPlaybackContract.ServerSabr,
                sameMedia = false,
                currentPlayWhenReady = false,
                initialPlayWhenReady = false,
            ),
        )
    }

    @Test
    fun `fresh live session starts at the server live edge`() {
        assertEquals(
            0L,
            replacementSourceStartTimeMs(
                sameMedia = true,
                live = true,
                reusingCurrentSource = false,
                currentMediaTimeMs = 43_200_000L,
                requestedStartTimeMs = 43_200_000L,
            ),
        )
        assertEquals(
            0L,
            replacementPlayerPositionMs(
                sameMedia = true,
                live = true,
                reusingCurrentSource = false,
                currentPositionMs = 20_000L,
                requestedPositionMs = 43_200_000L,
            ),
        )
    }

    @Test
    fun `active live session keeps its current positions when reused`() {
        assertEquals(
            43_200_000L,
            replacementSourceStartTimeMs(
                sameMedia = true,
                live = true,
                reusingCurrentSource = true,
                currentMediaTimeMs = 43_200_000L,
                requestedStartTimeMs = 0L,
            ),
        )
        assertEquals(
            20_000L,
            replacementPlayerPositionMs(
                sameMedia = true,
                live = true,
                reusingCurrentSource = true,
                currentPositionMs = 20_000L,
                requestedPositionMs = 0L,
            ),
        )
    }

    @Test
    fun `fresh VOD recovery preserves its current position`() {
        assertEquals(
            120_000L,
            replacementSourceStartTimeMs(
                sameMedia = true,
                live = false,
                reusingCurrentSource = false,
                currentMediaTimeMs = 120_000L,
                requestedStartTimeMs = 0L,
            ),
        )
        assertEquals(
            120_000L,
            replacementPlayerPositionMs(
                sameMedia = true,
                live = false,
                reusingCurrentSource = false,
                currentPositionMs = 120_000L,
                requestedPositionMs = 0L,
            ),
        )
    }
}
