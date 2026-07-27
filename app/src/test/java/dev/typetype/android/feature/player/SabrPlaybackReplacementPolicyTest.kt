package dev.typetype.android.feature.player

import dev.typetype.android.domain.stream.StreamPlaybackContract
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
}
