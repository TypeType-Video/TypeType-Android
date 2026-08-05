package dev.typetype.android.feature.player.components

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Rational
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import dev.typetype.android.MainActivity
import dev.typetype.android.services.PlaybackService
import dev.typetype.android.services.createSyntheticH264Video
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@RunWith(AndroidJUnit4::class)
@UnstableApi
class PictureInPictureTransitionTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test
    fun mainActivityEntersPipWithoutCreatingAnotherActivity() {
        val context = instrumentation.targetContext
        assertTrue(
            context.packageManager.hasSystemFeature(
                PackageManager.FEATURE_PICTURE_IN_PICTURE,
            ),
        )
        val activity = instrumentation.startActivitySync(
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )

        try {
            val entered = AtomicBoolean()
            instrumentation.runOnMainSync {
                entered.set(
                    activity.enterPictureInPictureMode(
                        PictureInPictureApi26.buildParams(
                            activity = activity,
                            aspectRatio = Rational(16, 9),
                            autoEnter = false,
                            isPlaying = true,
                            sourceRect = null,
                        ),
                    ),
                )
            }
            assertTrue(entered.get())
            instrumentation.waitForIdleSync()

            assertTrue(readOnMainThread { activity.isInPictureInPictureMode })
            val pausedMainActivities = readOnMainThread {
                ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(Stage.PAUSED)
                    .filterIsInstance<MainActivity>()
            }
            assertEquals(listOf(activity), pausedMainActivities)
        } finally {
            instrumentation.runOnMainSync { activity.finishAndRemoveTask() }
        }
    }

    @Test
    fun videoPlaybackContinuesAndPausesThroughTheSharedSessionInPip() {
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
            val positionBeforePip = readOnMainThread { controller.currentPosition }

            instrumentation.runOnMainSync {
                activity.enterPictureInPictureMode(
                    PictureInPictureApi26.buildParams(
                        activity = activity,
                        aspectRatio = Rational(16, 9),
                        autoEnter = false,
                        isPlaying = true,
                        sourceRect = null,
                    ),
                )
            }
            assertTrue(waitForActivityPip(activity))
            assertTrue(
                waitForController(controller) {
                    it.playWhenReady && it.currentPosition >= positionBeforePip + 250L
                },
            )

            instrumentation.runOnMainSync { controller.pause() }
            assertTrue(waitForController(controller) { !it.playWhenReady })
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

    private fun waitForActivityPip(activity: MainActivity): Boolean {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
        while (System.nanoTime() < deadline) {
            if (readOnMainThread { activity.isInPictureInPictureMode }) return true
            Thread.sleep(50)
        }
        return readOnMainThread { activity.isInPictureInPictureMode }
    }

    private fun waitForController(
        controller: MediaController,
        condition: (MediaController) -> Boolean,
    ): Boolean {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
        while (System.nanoTime() < deadline) {
            if (readOnMainThread { condition(controller) }) return true
            Thread.sleep(50)
        }
        return readOnMainThread { condition(controller) }
    }
}
