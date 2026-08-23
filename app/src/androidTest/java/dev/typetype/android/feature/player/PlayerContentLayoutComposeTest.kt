package dev.typetype.android.feature.player

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test

class PlayerContentLayoutComposeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun wideTabletPlacesDetailsBesideThePlayer() {
        setLayout(width = 1280.dp, height = 800.dp)

        val viewport = bounds(VIEWPORT_TAG)
        val details = bounds(DETAILS_TAG)

        assertTrue(viewport.right <= details.left)
        assertTrue(details.top == viewport.top)
        assertNodeCount(PLAYER_TWO_PANE_LAYOUT_TAG, 1)
        assertNodeCount(PLAYER_SINGLE_COLUMN_LAYOUT_TAG, 0)
    }

    @Test
    fun landscapePhoneKeepsDetailsBelowThePlayer() {
        setLayout(width = 840.dp, height = 393.dp)

        assertNodeCount(PLAYER_SINGLE_COLUMN_LAYOUT_TAG, 1)
        assertNodeCount(PLAYER_TWO_PANE_LAYOUT_TAG, 0)
    }

    @Test
    fun portraitTabletKeepsDetailsBelowThePlayer() {
        setLayout(width = 1067.dp, height = 1440.dp)

        val viewport = bounds(VIEWPORT_TAG)
        val details = bounds(DETAILS_TAG)

        assertTrue(viewport.bottom <= details.top)
        assertNodeCount(PLAYER_SINGLE_COLUMN_LAYOUT_TAG, 1)
        assertNodeCount(PLAYER_TWO_PANE_LAYOUT_TAG, 0)
    }

    @Test
    fun fullscreenLayoutRetainsTheNativePlayerView() {
        var isFullscreen by mutableStateOf(false)
        var createdViews = 0
        lateinit var firstView: PlayerView
        lateinit var currentView: PlayerView
        composeRule.setContent {
            MaterialTheme {
                Box(Modifier.requiredWidth(400.dp).requiredHeight(600.dp)) {
                    PlayerContentLayout(
                        isFullscreen = isFullscreen,
                        modifier = Modifier.fillMaxSize(),
                        viewport = { modifier ->
                            AndroidView(
                                factory = { context ->
                                    PlayerView(context).also {
                                        createdViews += 1
                                        if (createdViews == 1) firstView = it
                                    }
                                },
                                update = { currentView = it },
                                modifier = modifier,
                            )
                        },
                        details = { Box(it.height(300.dp)) },
                    )
                }
            }
        }
        composeRule.waitForIdle()
        assertEquals(1, createdViews)

        composeRule.runOnIdle { isFullscreen = true }
        composeRule.waitForIdle()
        assertEquals(1, createdViews)
        assertSame(firstView, currentView)

        composeRule.runOnIdle { isFullscreen = false }
        composeRule.waitForIdle()
        assertEquals(1, createdViews)
    }

    private fun setLayout(width: Dp, height: Dp) {
        composeRule.setContent {
            MaterialTheme {
                Box(Modifier.requiredWidth(width).requiredHeight(height)) {
                    PlayerContentLayout(
                        isFullscreen = false,
                        modifier = Modifier.fillMaxSize(),
                        viewport = { Box(it.testTag(VIEWPORT_TAG)) },
                        details = { detailsModifier ->
                            Box(detailsModifier.testTag(DETAILS_TAG)) {
                                Box(Modifier.height(600.dp))
                            }
                        },
                    )
                }
            }
        }
    }

    private fun bounds(tag: String) = composeRule
        .onNodeWithTag(tag)
        .fetchSemanticsNode()
        .boundsInRoot

    private fun assertNodeCount(tag: String, expected: Int) {
        val count = composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().size
        assertEquals(expected, count)
    }
}

private const val VIEWPORT_TAG = "player_viewport"
private const val DETAILS_TAG = "player_details"
