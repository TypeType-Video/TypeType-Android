package dev.typetype.android.feature.player

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import dev.typetype.android.core.ui.theme.TypeTypeTheme
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ShortsPlayerOptionsButtonTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun shortsExposePlaybackOptions() {
        val opened = AtomicBoolean(false)
        composeRule.setContent {
            TypeTypeTheme {
                ShortsPlaybackOptionsButton(onClick = { opened.set(true) })
            }
        }

        composeRule.onNodeWithContentDescription("Playback options").performClick()

        assertTrue(opened.get())
    }
}
