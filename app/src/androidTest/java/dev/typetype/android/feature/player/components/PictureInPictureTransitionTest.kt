package dev.typetype.android.feature.player.components

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Rational
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import dev.typetype.android.MainActivity
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@RunWith(AndroidJUnit4::class)
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

    private fun <T> readOnMainThread(block: () -> T): T {
        var result: Result<T>? = null
        instrumentation.runOnMainSync { result = runCatching(block) }
        return requireNotNull(result).getOrThrow()
    }
}
