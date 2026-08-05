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
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import dev.typetype.android.MainActivity
import dev.typetype.android.R
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
        ) as MainActivity

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
                            audioOnlyAvailable = true,
                            sourceRect = null,
                        ),
                    ),
                )
            }
            assertTrue(entered.get())
            instrumentation.waitForIdleSync()

            assertTrue(readOnMainThread { activity.isInPictureInPictureMode })
            val pausedMainActivities = waitForPausedMainActivities()
            assertEquals(listOf(activity), pausedMainActivities)
        } finally {
            instrumentation.runOnMainSync { activity.finishAndRemoveTask() }
            assertTrue(waitForActivityDestroyed(activity))
        }
    }

    @Test
    fun videoPlaybackContinuesAndPipActionControlsTheSharedSession() {
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

            val params = PictureInPictureApi26.buildParams(
                activity = activity,
                aspectRatio = Rational(16, 9),
                autoEnter = false,
                isPlaying = true,
                audioOnlyAvailable = true,
                sourceRect = null,
            )
            instrumentation.runOnMainSync { activity.enterPictureInPictureMode(params) }
            assertTrue(waitForActivityPip(activity))
            assertTrue(
                waitForController(controller) {
                    it.playWhenReady && it.currentPosition >= positionBeforePip + 250L
                },
            )

            val pipRevealPoint = waitForPipRevealPoint(activity)
            val pauseAction = waitForPipAction(R.string.player_action_pause, pipRevealPoint)
            pauseAction.click()
            assertTrue(waitForController(controller) { !it.playWhenReady })
            val device = UiDevice.getInstance(instrumentation)
            val pauseLabel = instrumentation.targetContext.getString(R.string.player_action_pause)
            device.wait(Until.gone(By.desc(pauseLabel)), PIP_MENU_TIMEOUT_MILLIS)
            waitForPipAction(R.string.player_action_play, pipRevealPoint).click()
            assertTrue(waitForController(controller) { it.playWhenReady })
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

    private fun waitForActivityDestroyed(activity: MainActivity): Boolean {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
        while (System.nanoTime() < deadline) {
            if (readOnMainThread { activity.isDestroyed }) {
                UiDevice.getInstance(instrumentation).waitForIdle()
                return true
            }
            Thread.sleep(50)
        }
        return readOnMainThread { activity.isDestroyed }
    }

    private fun waitForPausedMainActivities(): List<MainActivity> {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
        var activities = emptyList<MainActivity>()
        while (System.nanoTime() < deadline) {
            activities = readOnMainThread {
                ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(Stage.PAUSED)
                    .filterIsInstance<MainActivity>()
            }
            if (activities.isNotEmpty()) return activities
            Thread.sleep(50)
        }
        return activities
    }

    private fun waitForPipRevealPoint(activity: MainActivity): Pair<Int, Int> {
        val displayWidth = UiDevice.getInstance(instrumentation).displayWidth
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
        var bounds = readPipBounds(activity)
        while (System.nanoTime() < deadline) {
            bounds = readPipBounds(activity)
            if (bounds.width in 1 until displayWidth) {
                return bounds.x + bounds.width / 2 to bounds.y + bounds.height / 2
            }
            Thread.sleep(50)
        }
        require(bounds.width in 1 until displayWidth) { "PiP window was not resized" }
        return bounds.x + bounds.width / 2 to bounds.y + bounds.height / 2
    }

    private fun readPipBounds(activity: MainActivity): PipBounds = readOnMainThread {
        val location = IntArray(2)
        val view = activity.window.decorView
        view.getLocationOnScreen(location)
        PipBounds(location[0], location[1], view.width, view.height)
    }

    private fun waitForPipAction(labelResource: Int, revealPoint: Pair<Int, Int>): UiObject2 {
        val device = UiDevice.getInstance(instrumentation)
        val label = instrumentation.targetContext.getString(labelResource)
        device.wait(Until.findObject(By.desc(label)), PIP_MENU_TIMEOUT_MILLIS)?.let { return it }
        device.click(revealPoint.first, revealPoint.second)
        return requireNotNull(
            device.wait(Until.findObject(By.desc(label)), PIP_ACTION_TIMEOUT_MILLIS),
        ) { "PiP action is not visible: $label" }
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

    private companion object {
        const val PIP_MENU_TIMEOUT_MILLIS = 1_000L
        const val PIP_ACTION_TIMEOUT_MILLIS = 10_000L
    }

    private data class PipBounds(
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int,
    )
}
