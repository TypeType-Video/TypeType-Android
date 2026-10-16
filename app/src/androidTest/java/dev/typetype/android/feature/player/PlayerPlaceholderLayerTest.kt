package dev.typetype.android.feature.player

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PlayerPlaceholderLayerTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun errorArtworkDisappearsWhenCollapsedAndReturnsWhenExpanded() {
        val progress = mutableFloatStateOf(0f)
        rule.setContent {
            Box(Modifier.size(120.dp).background(Color.Blue).testTag("viewport")) {
                PlayerPlaceholderLayer({ progress.floatValue }) {
                    Box(Modifier.size(120.dp).background(Color.Red))
                }
            }
        }
        assertCenter(Color.Red)
        rule.runOnIdle { progress.floatValue = 1f }
        assertCenter(Color.Blue)
        rule.runOnIdle { progress.floatValue = 0f }
        assertCenter(Color.Red)
    }

    private fun assertCenter(expected: Color) {
        rule.waitForIdle()
        val pixels = rule.onNodeWithTag("viewport").captureToImage().toPixelMap()
        assertEquals(expected, pixels[pixels.width / 2, pixels.height / 2])
    }
}
