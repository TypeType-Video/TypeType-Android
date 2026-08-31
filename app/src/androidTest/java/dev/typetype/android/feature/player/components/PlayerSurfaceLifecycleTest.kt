package dev.typetype.android.feature.player.components

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PlayerSurfaceLifecycleTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun pausedBackgroundCyclePreservesSurfaceWithoutScreenOff() {
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
            assertEquals("video:0", surfaceKey)
        }
    }

    @Test
    fun onlyScreenOffRequestsSurfaceRefresh() {
        val gate = PlayerSurfaceRefreshGate()

        assertFalse(gate.consumeScreenOff())
        gate.markScreenOff()
        assertTrue(gate.consumeScreenOff())
        assertFalse(gate.consumeScreenOff())
    }
}
