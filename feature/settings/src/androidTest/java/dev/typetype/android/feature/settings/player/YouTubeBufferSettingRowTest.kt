package dev.typetype.android.feature.settings.player

import android.graphics.Bitmap
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import dev.typetype.android.core.ui.theme.TypeTypeTheme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class YouTubeBufferSettingRowTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun allFourBufferTargetsCanBeSelected() {
        val selected = mutableIntStateOf(30)
        composeRule.setContent {
            TypeTypeTheme {
                YouTubeBufferSettingRow(selected.intValue) { selected.intValue = it }
            }
        }
        composeRule.onNodeWithText("30 seconds").assertIsDisplayed()
        for (seconds in listOf(10, 30, 60, 120)) {
            composeRule.onNodeWithText("YouTube buffer").performClick()
            composeRule.waitForIdle()
            if (seconds == 10) saveOptionsScreenshot()
            composeRule.onNodeWithText("$seconds seconds").performClick()
            composeRule.onNodeWithText("$seconds seconds").assertIsDisplayed()
            composeRule.runOnIdle { assertEquals(seconds, selected.intValue) }
        }
    }

    private fun saveOptionsScreenshot() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val screenshot = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        val file = File(instrumentation.targetContext.externalCacheDir, "youtube-buffer-options.png")
        file.outputStream().use { screenshot.compress(Bitmap.CompressFormat.PNG, 100, it) }
        screenshot.recycle()
    }
}
