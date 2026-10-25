package dev.typetype.android.feature.player.components

import android.os.Looper
import android.view.View
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.TrackGroup
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import androidx.media3.ui.compose.state.PresentationState
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@androidx.annotation.OptIn(markerClass = [UnstableApi::class])
class PlayerSurfaceRetentionTest {
    @Test
    fun retainedSurfaceDoesNotRestoreTheShutterWhenTracksReset() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        lateinit var defaultView: PlayerView
        lateinit var retainedView: PlayerView
        lateinit var defaultPlayer: SurfaceStatePlayer
        lateinit var retainedPlayer: SurfaceStatePlayer
        lateinit var presentationState: PresentationState
        val observationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

        instrumentation.runOnMainSync {
            val context = ApplicationProvider.getApplicationContext<android.content.Context>()
            defaultPlayer = SurfaceStatePlayer(Looper.getMainLooper())
            retainedPlayer = SurfaceStatePlayer(Looper.getMainLooper())
            defaultView = PlayerView(context).apply { player = defaultPlayer }
            retainedView = PlayerView(context).apply {
                retainContentAcrossPlayerResets()
                player = retainedPlayer
            }
            presentationState = PresentationState(keepContentOnReset = true)
            observationScope.launch { presentationState.observe(retainedPlayer) }
            defaultPlayer.renderFirstFrame()
            retainedPlayer.renderFirstFrame()
        }
        instrumentation.waitForIdleSync()

        instrumentation.runOnMainSync {
            assertNotEquals(View.VISIBLE, defaultView.shutter().visibility)
            assertNotEquals(View.VISIBLE, retainedView.shutter().visibility)
            assertEquals(false, presentationState.coverSurface)
            retainedPlayer.startBuffering()
            defaultPlayer.resetTracks()
            retainedPlayer.resetTracks()
        }
        instrumentation.waitForIdleSync()

        instrumentation.runOnMainSync {
            assertEquals(View.VISIBLE, defaultView.shutter().visibility)
            assertNotEquals(View.VISIBLE, retainedView.shutter().visibility)
            assertEquals(false, presentationState.coverSurface)
            retainedPlayer.restoreTracksAndRender()
        }
        instrumentation.waitForIdleSync()

        instrumentation.runOnMainSync {
            retainedPlayer.startBuffering()
            retainedPlayer.resetTracks()
        }
        instrumentation.waitForIdleSync()

        instrumentation.runOnMainSync {
            assertNotEquals(View.VISIBLE, retainedView.shutter().visibility)
            assertEquals(false, presentationState.coverSurface)
            defaultView.player = null
            retainedView.player = null
            defaultPlayer.release()
            retainedPlayer.release()
            observationScope.cancel()
        }
    }

    private fun PlayerView.shutter(): View =
        requireNotNull(findViewById(androidx.media3.ui.R.id.exo_shutter))
}

private class SurfaceStatePlayer(looper: Looper) : SimpleBasePlayer(looper) {
    private var state = stateWithTracks(videoTracks(), renderedFirstFrame = false)

    override fun getState(): State = state

    override fun handleSetVideoOutput(videoOutput: Any): ListenableFuture<*> =
        Futures.immediateVoidFuture()

    override fun handleClearVideoOutput(videoOutput: Any?): ListenableFuture<*> =
        Futures.immediateVoidFuture()

    override fun handleRelease(): ListenableFuture<*> = Futures.immediateVoidFuture()

    fun renderFirstFrame() {
        state = state.buildUpon().setNewlyRenderedFirstFrame(true).build()
        invalidateState()
    }

    fun resetTracks() {
        state = stateWithTracks(
            Tracks.EMPTY,
            renderedFirstFrame = false,
            playbackState = Player.STATE_BUFFERING,
        )
        invalidateState()
    }

    fun startBuffering() {
        state = stateWithTracks(
            videoTracks(),
            renderedFirstFrame = false,
            playbackState = Player.STATE_BUFFERING,
        )
        invalidateState()
    }

    fun restoreTracksAndRender() {
        state = stateWithTracks(videoTracks(), renderedFirstFrame = true)
        invalidateState()
    }
}

private fun stateWithTracks(
    tracks: Tracks,
    renderedFirstFrame: Boolean,
    playbackState: Int = Player.STATE_READY,
): SimpleBasePlayer.State {
    val item = SimpleBasePlayer.MediaItemData.Builder(MEDIA_UID)
        .setMediaItem(MediaItem.Builder().setMediaId("video").build())
        .setTracks(tracks)
        .build()
    return SimpleBasePlayer.State.Builder()
        .setAvailableCommands(Player.Commands.Builder().addAllCommands().build())
        .setPlaybackState(playbackState)
        .setPlaylist(listOf(item))
        .setNewlyRenderedFirstFrame(renderedFirstFrame)
        .build()
}

private fun videoTracks(): Tracks {
    val format = Format.Builder().setSampleMimeType(MimeTypes.VIDEO_H264).build()
    val group = TrackGroup(format)
    return Tracks(
        listOf(
            Tracks.Group(
                group,
                false,
                intArrayOf(C.FORMAT_HANDLED),
                booleanArrayOf(true),
            ),
        ),
    )
}

private const val MEDIA_UID = "surface-content"
