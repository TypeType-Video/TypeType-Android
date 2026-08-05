package dev.typetype.android.feature.player

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
