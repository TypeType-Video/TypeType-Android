package dev.typetype.android.feature.player

import dev.typetype.android.feature.player.error.StreamErrorClass
import dev.typetype.android.feature.player.error.StreamErrorKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlayerRetryTest {
    @Test
    fun `playback retry changes the binding generation`() {
        val initial = PlayerState(playbackBindGeneration = 4L)

        val retried = initial.retryPlayback()

        assertEquals(5L, retried.playbackBindGeneration)
    }

    @Test
    fun `SABR preparation retry clears the blocking error`() {
        val initial = PlayerState(
            playbackBindGeneration = 4L,
            error = StreamErrorClass(StreamErrorKind.SabrUnavailable, rawMessage = null),
        )

        val retried = initial.retryPlayback()

        assertEquals(5L, retried.playbackBindGeneration)
        assertNull(retried.error)
    }
}
