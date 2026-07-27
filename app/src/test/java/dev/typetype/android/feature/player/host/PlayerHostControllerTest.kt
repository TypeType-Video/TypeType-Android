package dev.typetype.android.feature.player.host

import dev.typetype.android.domain.playback.PlaybackQueueController
import dev.typetype.android.domain.playback.PlaybackQueueEntry
import dev.typetype.android.domain.playback.PlaybackQueueSnapshot
import dev.typetype.android.domain.playback.PlaybackQueueState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerHostControllerTest {
    @Test
    fun `restored video opens paused in the mini player`() {
        val controller = PlayerHostController(FakePlaybackQueueController())

        controller.restoreVideo("https://youtube.com/watch?v=video", 42_000L)

        val state = controller.state.value
        assertEquals("https://youtube.com/watch?v=video", state.videoUrl)
        assertEquals(42_000L, state.resumePositionMillis)
        assertEquals(PlayerHostTarget.Mini, state.target)
        assertFalse(state.initialPlayWhenReady)
    }

    @Test
    fun `opening another video discards restored launch state`() {
        val controller = PlayerHostController(FakePlaybackQueueController())
        controller.restoreVideo("first", 42_000L)

        controller.openVideo("second")

        val state = controller.state.value
        assertEquals("second", state.videoUrl)
        assertNull(state.resumePositionMillis)
        assertEquals(PlayerHostTarget.Expanded, state.target)
        assertTrue(state.initialPlayWhenReady)
    }

    @Test
    fun `hiding the player discards transient resume state`() {
        val controller = PlayerHostController(FakePlaybackQueueController())
        controller.restoreVideo("video", 42_000L)

        controller.hide()

        val state = controller.state.value
        assertNull(state.videoUrl)
        assertNull(state.resumePositionMillis)
        assertEquals(PlayerHostTarget.Hidden, state.target)
        assertTrue(state.initialPlayWhenReady)
    }
}

private class FakePlaybackQueueController : PlaybackQueueController {
    override val state: StateFlow<PlaybackQueueState> = MutableStateFlow(PlaybackQueueState())
    override fun start(title: String, entries: List<PlaybackQueueEntry>, shuffle: Boolean) = Unit
    override fun restore(snapshot: PlaybackQueueSnapshot) = Unit
    override fun clear() = Unit
}
