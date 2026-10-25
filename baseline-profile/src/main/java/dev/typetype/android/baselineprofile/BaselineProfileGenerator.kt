package dev.typetype.android.baselineprofile

import android.content.Intent
import android.net.Uri
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun startup() = baselineProfileRule.collect(
        packageName = TARGET_PACKAGE,
        includeInStartupProfile = true,
    ) {
        pressHome()
        startActivityAndWait()
        device.wait(Until.hasObject(By.text(HOME_TAB)), TIMEOUT_MS)
    }

    @Test
    fun browseAndPlay() = baselineProfileRule.collect(packageName = TARGET_PACKAGE) {
        pressHome()
        startActivityAndWait()
        device.wait(Until.hasObject(By.text(HOME_TAB)), TIMEOUT_MS)
        repeat(FLINGS) { flingUp() }
        device.findObject(By.text(SHORTS_TAB))?.click()
        device.waitForIdle()
        repeat(FLINGS) { flingUp() }
        device.pressBack()
        startActivityAndWait(
            Intent(Intent.ACTION_VIEW, Uri.parse(VIDEO_URL)).setPackage(TARGET_PACKAGE),
        )
        Thread.sleep(PLAYBACK_MS)
    }

    private fun MacrobenchmarkScope.flingUp() {
        device.swipe(
            device.displayWidth / 2,
            device.displayHeight * 4 / 5,
            device.displayWidth / 2,
            device.displayHeight / 5,
            FLING_STEPS,
        )
        device.waitForIdle()
    }

    private companion object {
        const val HOME_TAB = "Home"
        const val SHORTS_TAB = "Shorts"
        const val VIDEO_URL = "https://www.youtube.com/watch?v=6j-gQmaZ9Zk"
        const val TIMEOUT_MS = 10_000L
        const val PLAYBACK_MS = 8_000L
        const val FLINGS = 4
        const val FLING_STEPS = 12
    }
}
