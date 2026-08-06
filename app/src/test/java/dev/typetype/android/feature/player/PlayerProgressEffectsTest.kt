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
                currentMediaId = CURRENT_VIDEO,
                currentVideoUrl = CURRENT_VIDEO,
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
                currentMediaId = CURRENT_VIDEO,
                currentVideoUrl = CURRENT_VIDEO,
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
                currentMediaId = CURRENT_VIDEO,
                currentVideoUrl = CURRENT_VIDEO,
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
                currentMediaId = CURRENT_VIDEO,
                currentVideoUrl = CURRENT_VIDEO,
                targetUrl = "https://example.com/watch?v=next",
            ),
        )
        assertFalse(
            shouldStartAutoplayCountdown(
                playbackState = Player.STATE_ENDED,
                playWhenReady = true,
                enabled = true,
                currentMediaId = CURRENT_VIDEO,
                currentVideoUrl = CURRENT_VIDEO,
                targetUrl = null,
            ),
        )
    }

    @Test
    fun `new video ignores the previous ended media item`() {
        assertFalse(
            shouldStartAutoplayCountdown(
                playbackState = Player.STATE_ENDED,
                playWhenReady = true,
                enabled = true,
                currentMediaId = CURRENT_VIDEO,
                currentVideoUrl = "https://example.com/watch?v=new",
                targetUrl = "https://example.com/watch?v=next",
            ),
        )
    }

    private companion object {
        const val CURRENT_VIDEO = "https://example.com/watch?v=current"
    }
}
