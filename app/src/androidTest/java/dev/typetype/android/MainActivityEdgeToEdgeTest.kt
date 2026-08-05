package dev.typetype.android

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.view.View
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
@RunWith(AndroidJUnit4::class)
class MainActivityEdgeToEdgeTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test
    fun contentOccupiesTheDecorBoundsAndReceivesSystemBarInsets() {
        val context = instrumentation.targetContext
        val activity = instrumentation.startActivitySync(
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        ) as MainActivity

        try {
            val observedInsets = AtomicReference<WindowInsetsCompat>()
            val content = readOnMainThread {
                activity.findViewById<View>(android.R.id.content)
            }
            instrumentation.runOnMainSync {
                ViewCompat.setOnApplyWindowInsetsListener(content) { _, insets ->
                    observedInsets.set(insets)
                    insets
                }
                ViewCompat.requestApplyInsets(content)
            }

            val systemBars = waitForInsets(observedInsets)
            val decorBounds = readBounds(activity.window.decorView)
            val contentBounds = readBounds(content)

            assertNotNull(systemBars)
            assertTrue(requireNotNull(systemBars).top > 0)
            assertEquals(decorBounds, contentBounds)
            @Suppress("DEPRECATION")
            assertEquals(Color.TRANSPARENT, activity.window.statusBarColor)
        } finally {
            instrumentation.runOnMainSync { activity.finishAndRemoveTask() }
        }
    }

    private fun waitForInsets(reference: AtomicReference<WindowInsetsCompat>): Insets? {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
        while (System.nanoTime() < deadline) {
            reference.get()?.let {
                return it.getInsets(WindowInsetsCompat.Type.systemBars())
            }
            Thread.sleep(50)
        }
        return reference.get()?.getInsets(WindowInsetsCompat.Type.systemBars())
    }

    private fun readBounds(view: View): List<Int> = readOnMainThread {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        listOf(location[0], location[1], view.width, view.height)
    }

    private fun <T> readOnMainThread(block: () -> T): T {
        var result: Result<T>? = null
        instrumentation.runOnMainSync { result = runCatching(block) }
        return requireNotNull(result).getOrThrow()
    }
}
