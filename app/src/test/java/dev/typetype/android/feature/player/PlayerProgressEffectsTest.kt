package dev.typetype.android.feature.player

import androidx.media3.common.Player
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerProgressEffectsTest {
    @Test
    fun `autoplay starts after a naturally completed video`() {
        assertTrue(
            shouldAutoplayNext(
                playbackState = Player.STATE_ENDED,
                playWhenReady = true,
                explicitQueueActive = false,
                autoplayEnabled = true,
                nextVideoUrl = "https://example.com/watch?v=next",
            ),
        )
    }

    @Test
    fun `autoplay stays stopped after the sleep timer pauses playback`() {
        assertFalse(
            shouldAutoplayNext(
                playbackState = Player.STATE_ENDED,
                playWhenReady = false,
                explicitQueueActive = false,
                autoplayEnabled = true,
                nextVideoUrl = "https://example.com/watch?v=next",
            ),
        )
    }

    @Test
    fun `autoplay ignores non-terminal playback states`() {
        assertFalse(
            shouldAutoplayNext(
                playbackState = Player.STATE_READY,
                playWhenReady = true,
                explicitQueueActive = false,
                autoplayEnabled = true,
                nextVideoUrl = "https://example.com/watch?v=next",
            ),
        )
    }
}
