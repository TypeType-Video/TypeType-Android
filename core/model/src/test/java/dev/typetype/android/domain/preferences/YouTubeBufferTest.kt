package dev.typetype.android.domain.preferences

import org.junit.Assert.assertEquals
import org.junit.Test

class YouTubeBufferTest {
    @Test
    fun newPreferencesDefaultToThirtyAndOnlyFourChoicesAreAccepted() {
        assertEquals(30, AppPreferences().playerYouTubeBufferSeconds)
        assertEquals(listOf(10, 30, 60, 120), YOUTUBE_BUFFER_SECONDS)
        for (seconds in YOUTUBE_BUFFER_SECONDS) assertEquals(seconds, normalizeYouTubeBufferSeconds(seconds))
        for (seconds in listOf(-1, 0, 15, 121, Int.MAX_VALUE)) {
            assertEquals(30, normalizeYouTubeBufferSeconds(seconds))
        }
    }

    @Test
    fun bufferGoalsRespectChoicesPlaybackSpeedAndTheUpperBound() {
        assertEquals(120_000L, youTubeBufferGoalMs(120))
        assertEquals(10_000L, youTubeBufferGoalMs(10, 0.5f))
        assertEquals(60_000L, youTubeBufferGoalMs(30, 2f))
        assertEquals(120_000L, youTubeBufferGoalMs(120, 4f))
        assertEquals(30_000L, youTubeBufferGoalMs(30, Float.NaN))
    }
}
