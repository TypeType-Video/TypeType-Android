package dev.typetype.android.feature.player.components

import androidx.media3.common.Player
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerPlaybackStatusTest {
    @Test
    fun recoveryUsesBufferingPresentationAndBlocksPlayerInput() {
        val status = PlayerPlaybackStatus(
            playbackState = Player.STATE_IDLE,
            isPlaying = false,
            isLoading = false,
            error = null,
            isRecovering = true,
        )

        assertTrue(status.isBuffering)
        assertFalse(status.acceptsInput)
    }

    @Test
    fun readyPlaybackAcceptsInput() {
        val status = PlayerPlaybackStatus(
            playbackState = Player.STATE_READY,
            isPlaying = true,
            isLoading = false,
            error = null,
            isRecovering = false,
        )

        assertFalse(status.isBuffering)
        assertTrue(status.acceptsInput)
    }
}
