package dev.typetype.android.feature.player

import androidx.media3.common.Player
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerProgressEffectsTest {
    @Test
    fun `countdown starts after a naturally completed video`() {
        assertTrue(
            shouldStartAutoplayCountdown(
                playbackState = Player.STATE_ENDED,
                playWhenReady = true,
                enabled = true,
                targetUrl = "https://example.com/watch?v=next",
            ),
        )
    }

    @Test
    fun `countdown stays stopped after the sleep timer pauses playback`() {
        assertFalse(
            shouldStartAutoplayCountdown(
                playbackState = Player.STATE_ENDED,
                playWhenReady = false,
                enabled = true,
                targetUrl = "https://example.com/watch?v=next",
            ),
        )
    }

    @Test
    fun `countdown ignores non-terminal playback states`() {
        assertFalse(
            shouldStartAutoplayCountdown(
                playbackState = Player.STATE_READY,
                playWhenReady = true,
                enabled = true,
                targetUrl = "https://example.com/watch?v=next",
            ),
        )
    }

    @Test
    fun `countdown requires autoplay and a target`() {
        assertFalse(
            shouldStartAutoplayCountdown(
                playbackState = Player.STATE_ENDED,
                playWhenReady = true,
                enabled = false,
                targetUrl = "https://example.com/watch?v=next",
            ),
        )
        assertFalse(
            shouldStartAutoplayCountdown(
                playbackState = Player.STATE_ENDED,
                playWhenReady = true,
                enabled = true,
                targetUrl = null,
            ),
        )
    }
}
