package dev.typetype.android.services

import androidx.media3.common.Player
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackQueueAutoplayControllerTest {
    @Test
    fun `completed queue item can start autoplay`() {
        assertEquals(
            QueueAutoplayDecision.StartCountdown,
            decideQueueAutoplay(
                playbackState = Player.STATE_ENDED,
                playWhenReady = true,
                enabled = true,
                countdownSeconds = 10,
                skipCountdown = false,
                currentMediaId = "current",
                nextVideoUrl = "next",
                dismissedMediaId = null,
            ),
        )
    }

    @Test
    fun `cancelled item stays cancelled while it remains current`() {
        assertEquals(
            QueueAutoplayDecision.None,
            decideQueueAutoplay(
                playbackState = Player.STATE_ENDED,
                playWhenReady = true,
                enabled = true,
                countdownSeconds = 10,
                skipCountdown = false,
                currentMediaId = "current",
                nextVideoUrl = "next",
                dismissedMediaId = "current",
            ),
        )
    }

    @Test
    fun `autoplay requires a natural end and a next item`() {
        assertEquals(
            QueueAutoplayDecision.None,
            decideQueueAutoplay(
                playbackState = Player.STATE_READY,
                playWhenReady = true,
                enabled = true,
                countdownSeconds = 10,
                skipCountdown = false,
                currentMediaId = "current",
                nextVideoUrl = "next",
                dismissedMediaId = null,
            ),
        )
        assertEquals(
            QueueAutoplayDecision.None,
            decideQueueAutoplay(
                playbackState = Player.STATE_ENDED,
                playWhenReady = false,
                enabled = true,
                countdownSeconds = 10,
                skipCountdown = false,
                currentMediaId = "current",
                nextVideoUrl = "next",
                dismissedMediaId = null,
            ),
        )
        assertEquals(
            QueueAutoplayDecision.None,
            decideQueueAutoplay(
                playbackState = Player.STATE_ENDED,
                playWhenReady = true,
                enabled = true,
                countdownSeconds = 10,
                skipCountdown = false,
                currentMediaId = "current",
                nextVideoUrl = null,
                dismissedMediaId = null,
            ),
        )
    }

    @Test
    fun `zero delay and playlist skip advance immediately`() {
        assertEquals(
            QueueAutoplayDecision.AdvanceImmediately,
            decideQueueAutoplay(
                playbackState = Player.STATE_ENDED,
                playWhenReady = true,
                enabled = true,
                countdownSeconds = 0,
                skipCountdown = false,
                currentMediaId = "current",
                nextVideoUrl = "next",
                dismissedMediaId = null,
            ),
        )
        assertEquals(
            QueueAutoplayDecision.AdvanceImmediately,
            decideQueueAutoplay(
                playbackState = Player.STATE_ENDED,
                playWhenReady = true,
                enabled = true,
                countdownSeconds = 10,
                skipCountdown = true,
                currentMediaId = "current",
                nextVideoUrl = "next",
                dismissedMediaId = null,
            ),
        )
    }
}
