package dev.typetype.android.services

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SabrPlaybackContractDataSourceTest {
    @Test
    fun `recognizes only explicit SABR playback payload paths`() {
        assertTrue(
            listOf("api", "sabr", "playback", "session", "manifest")
                .isSabrPlaybackPayloadPath(),
        )
        assertTrue(
            listOf("sabr", "playback", "session", "137", "init")
                .isSabrPlaybackPayloadPath(),
        )
        assertTrue(
            listOf("api", "sabr", "playback", "session", "140", "segment", "2")
                .isSabrPlaybackPayloadPath(),
        )

        assertFalse(listOf("streams", "manifest").isSabrPlaybackPayloadPath())
        assertFalse(
            listOf("sabr", "playback", "session", "bad", "init")
                .isSabrPlaybackPayloadPath(),
        )
        assertFalse(
            listOf("sabr", "playback", "session", "140", "segment", "bad")
                .isSabrPlaybackPayloadPath(),
        )
    }

    @Test
    fun `recognizes JSON content types case insensitively`() {
        assertTrue(mapOf("content-type" to listOf("application/json; charset=UTF-8")).hasJsonContentType())
        assertTrue(mapOf("Content-Type" to listOf("application/problem+json")).hasJsonContentType())
        assertFalse(mapOf("Content-Type" to listOf("video/mp4")).hasJsonContentType())
    }
}
