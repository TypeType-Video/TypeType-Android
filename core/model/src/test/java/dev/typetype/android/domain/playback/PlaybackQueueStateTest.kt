package dev.typetype.android.domain.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class PlaybackQueueStateTest {
    @Test
    fun previousEntryUsesTheQueueOrder() {
        val entries = listOf(entry("first"), entry("second"), entry("third"))

        val state = PlaybackQueueState(entries = entries, currentIndex = 1)

        assertSame(entries[0], state.previous)
        assertSame(entries[2], state.next)
    }

    @Test
    fun repeatAllWrapsPreviousFromTheFirstEntry() {
        val entries = listOf(entry("first"), entry("second"))

        val state = PlaybackQueueState(
            entries = entries,
            currentIndex = 0,
            repeatMode = PlaybackRepeatMode.All,
        )

        assertSame(entries.last(), state.previous)
    }

    @Test
    fun repeatOffHasNoPreviousAtTheStart() {
        val state = PlaybackQueueState(
            entries = listOf(entry("first"), entry("second")),
            currentIndex = 0,
            repeatMode = PlaybackRepeatMode.Off,
        )

        assertNull(state.previous)
        assertEquals("second", state.next?.videoUrl)
    }

    private fun entry(url: String) = PlaybackQueueEntry(
        videoUrl = url,
        title = url,
        thumbnailUrl = "",
        durationSeconds = 1L,
        channelName = "channel",
    )
}
