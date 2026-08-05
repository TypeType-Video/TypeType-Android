package dev.typetype.android.feature.player.components

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PlayerSurfaceLifecycleTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun activeLifecycleDoesNotRecreateSurfaceUntilARealStop() {
        var surfaceKey = ""
        composeRule.setContent {
            surfaceKey = rememberPlayerSurfaceKey("video")
        }

        composeRule.runOnIdle {
            assertEquals("video:0", surfaceKey)
        }

        composeRule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)

        composeRule.runOnIdle {
            assertEquals("video:1", surfaceKey)
        }
    }
}
