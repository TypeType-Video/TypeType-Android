package dev.typetype.android.services

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.Lifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.typetype.android.MainActivity
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@UnstableApi
@RunWith(AndroidJUnit4::class)
class PlaybackBackgroundTransitionTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test
    fun videoPlaybackContinuesAfterTheTaskMovesToBackground() {
        val context = instrumentation.targetContext
        val controller = MediaController.Builder(
            context,
            SessionToken(context, ComponentName(context, PlaybackService::class.java)),
        ).buildAsync().get(10, TimeUnit.SECONDS)
        val video = createSyntheticH264Video(context)
        val activity = instrumentation.startActivitySync(
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        ) as MainActivity

        try {
            instrumentation.runOnMainSync {
                controller.setMediaItem(
                    MediaItem.Builder()
                        .setUri(Uri.fromFile(video))
                        .setMimeType(MimeTypes.VIDEO_MP4)
                        .build(),
                )
                controller.prepare()
                controller.play()
            }
            assertTrue(
                waitForController(controller) {
                    it.playbackState == Player.STATE_READY && it.currentPosition >= 250L
                },
            )
            val positionBeforeBackground = readOnMainThread { controller.currentPosition }

            assertTrue(readOnMainThread { activity.moveTaskToBack(true) })
            assertTrue(
                waitForMainThread {
                    !activity.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
                },
            )
            assertTrue(
                waitForController(controller) {
                    it.playWhenReady && it.currentPosition >= positionBeforeBackground + 400L
                },
            )
        } finally {
            instrumentation.runOnMainSync {
                controller.stop()
                controller.clearMediaItems()
                controller.release()
                activity.finishAndRemoveTask()
            }
            video.delete()
        }
    }

    private fun <T> readOnMainThread(block: () -> T): T {
        var result: Result<T>? = null
        instrumentation.runOnMainSync { result = runCatching(block) }
        return requireNotNull(result).getOrThrow()
    }

    private fun waitForMainThread(condition: () -> Boolean): Boolean {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
        while (System.nanoTime() < deadline) {
            if (readOnMainThread(condition)) return true
            Thread.sleep(50)
        }
        return readOnMainThread(condition)
    }

    private fun waitForController(
        controller: MediaController,
        condition: (MediaController) -> Boolean,
    ): Boolean = waitForMainThread { condition(controller) }
}
