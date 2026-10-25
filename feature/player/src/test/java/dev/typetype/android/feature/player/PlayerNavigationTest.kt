package dev.typetype.android.feature.player

import dev.typetype.android.domain.playback.PlaybackQueueEntry
import dev.typetype.android.domain.playback.PlaybackQueueState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerNavigationTest {
    @Test
    fun queueTargetsTakePrecedenceOverRelatedHistory() {
        val state = PlaybackQueueState(
            entries = listOf(entry("first"), entry("second"), entry("third")),
            currentIndex = 1,
        )

        val availability = playerNavigationAvailability(
            queue = state,
            relatedVideoCount = 0,
            hasPreviousHistory = false,
        )

        assertTrue(availability.previous)
        assertTrue(availability.next)
    }

    @Test
    fun normalPlaybackUsesHistoryAndRelatedVideos() {
        val availability = playerNavigationAvailability(
            queue = PlaybackQueueState(),
            relatedVideoCount = 1,
            hasPreviousHistory = true,
        )

        assertTrue(availability.previous)
        assertTrue(availability.next)
    }

    @Test
    fun normalPlaybackDisablesButtonsWithoutTargets() {
        val availability = playerNavigationAvailability(
            queue = PlaybackQueueState(),
            relatedVideoCount = 0,
            hasPreviousHistory = false,
        )

        assertFalse(availability.previous)
        assertFalse(availability.next)
    }

    private fun entry(url: String) = PlaybackQueueEntry(
        videoUrl = url,
        title = url,
        thumbnailUrl = "",
        durationSeconds = 1L,
        channelName = "channel",
    )
}
