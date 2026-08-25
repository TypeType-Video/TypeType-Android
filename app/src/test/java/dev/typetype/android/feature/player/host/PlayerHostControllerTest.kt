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
        assertEquals(state.requestStamp, state.playbackClearRequestStamp)
    }

    @Test
    fun `acknowledging a playback clear consumes only the matching request`() {
        val controller = PlayerHostController(FakePlaybackQueueController())
        controller.hide()
        val requestStamp = requireNotNull(controller.state.value.playbackClearRequestStamp)

        controller.acknowledgePlaybackClear(requestStamp - 1)
        assertEquals(requestStamp, controller.state.value.playbackClearRequestStamp)

        controller.acknowledgePlaybackClear(requestStamp)
        assertNull(controller.state.value.playbackClearRequestStamp)
    }

    @Test
    fun `opening video cancels an unhandled playback clear`() {
        val controller = PlayerHostController(FakePlaybackQueueController())
        controller.hide()

        controller.openVideo("video")

        assertNull(controller.state.value.playbackClearRequestStamp)
    }

    @Test
    fun `embedded playback keeps the host overlay hidden`() {
        val controller = PlayerHostController(FakePlaybackQueueController())

        controller.openEmbeddedVideo("short", autoplay = false)

        val state = controller.state.value
        assertEquals("short", state.videoUrl)
        assertEquals(PlayerHostTarget.Embedded, state.target)
        assertFalse(state.initialPlayWhenReady)
    }

    @Test
    fun `leaving Shorts without previous playback hides embedded playback`() {
        val controller = PlayerHostController(FakePlaybackQueueController())
        controller.openEmbeddedVideo("short", autoplay = true)
        controller.closeEmbeddedPlayback()
        assertEquals(PlayerHostTarget.Hidden, controller.state.value.target)

        controller.openVideo("video")
        controller.closeEmbeddedPlayback()
        assertEquals(PlayerHostTarget.Expanded, controller.state.value.target)
    }

    @Test
    fun `leaving Shorts restores the mini player that opened it`() {
        val controller = PlayerHostController(FakePlaybackQueueController())
        controller.openVideo("video")
        controller.minimize()

        controller.openEmbeddedVideo(
            url = "short",
            autoplay = true,
            returnPositionMillis = 18_000L,
            returnPlayWhenReady = true,
        )
        controller.openEmbeddedVideo(
            url = "next-short",
            autoplay = true,
            returnPositionMillis = 2_000L,
            returnPlayWhenReady = false,
        )
        controller.closeEmbeddedPlayback()

        assertEquals(PlayerHostTarget.Mini, controller.state.value.target)
        assertEquals("video", controller.state.value.videoUrl)
        assertEquals(18_000L, controller.state.value.resumePositionMillis)
        assertTrue(controller.state.value.initialPlayWhenReady)
    }

    @Test
    fun `leaving Shorts restores an expanded player`() {
        val controller = PlayerHostController(FakePlaybackQueueController())
        controller.openVideo("video")

        controller.openEmbeddedVideo("short", autoplay = true)
        controller.closeEmbeddedPlayback()

        assertEquals(PlayerHostTarget.Expanded, controller.state.value.target)
    }

    @Test
    fun `expanded Short collapses back into embedded playback`() {
        val controller = PlayerHostController(FakePlaybackQueueController())
        controller.openEmbeddedVideo("short", autoplay = true)

        controller.expand()
        assertEquals(PlayerHostTarget.Expanded, controller.state.value.target)

        controller.collapseExpanded()

        assertEquals(PlayerHostTarget.Embedded, controller.state.value.target)
        assertEquals("short", controller.state.value.videoUrl)
    }

    @Test
    fun `regular expanded video collapses into the mini player`() {
        val controller = PlayerHostController(FakePlaybackQueueController())
        controller.openVideo("video")

        controller.collapseExpanded()

        assertEquals(PlayerHostTarget.Mini, controller.state.value.target)
    }

    @Test
    fun `autoplay keeps the mini player minimized`() {
        val controller = PlayerHostController(FakePlaybackQueueController())
        controller.openVideo("first")
        controller.minimize()

        controller.continueWithVideo("second")

        val state = controller.state.value
        assertEquals("second", state.videoUrl)
        assertEquals(PlayerHostTarget.Mini, state.target)
        assertNull(state.resumePositionMillis)
        assertTrue(state.initialPlayWhenReady)
    }

    @Test
    fun `autoplay keeps the expanded player expanded`() {
        val controller = PlayerHostController(FakePlaybackQueueController())
        controller.openVideo("first")

        controller.continueWithVideo("second")

        assertEquals("second", controller.state.value.videoUrl)
        assertEquals(PlayerHostTarget.Expanded, controller.state.value.target)
    }
}

private class FakePlaybackQueueController : PlaybackQueueController {
    override val state: StateFlow<PlaybackQueueState> = MutableStateFlow(PlaybackQueueState())
    override fun start(title: String, entries: List<PlaybackQueueEntry>, shuffle: Boolean) = Unit
    override fun restore(snapshot: PlaybackQueueSnapshot) = Unit
    override fun clear() = Unit
}
