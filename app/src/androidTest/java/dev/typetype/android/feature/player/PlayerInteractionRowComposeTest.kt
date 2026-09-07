package dev.typetype.android.feature.player

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.typetype.android.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PlayerInteractionRowComposeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun tabletShowsLabelsAndLargerTouchTargets() {
        var commentClicks = 0
        setActions(720.dp) { commentClicks += 1 }

        val comments = composeRule.activity.getString(R.string.comments_title)
        composeRule.onNodeWithText(comments).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(comments)
            .assertHeightIsAtLeast(72.dp)
            .performClick()
        assertEquals(1, commentClicks)
        composeRule.onNodeWithText(
            composeRule.activity.getString(R.string.video_menu_share),
        ).assertIsDisplayed()
    }

    @Test
    fun narrowWindowRetainsCompactActions() {
        setActions(400.dp)

        val comments = composeRule.activity.getString(R.string.comments_title)
        composeRule.onNodeWithText(comments).assertDoesNotExist()
        composeRule.onNodeWithContentDescription(comments)
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
    }

    private fun setActions(width: Dp, onComments: () -> Unit = {}) {
        composeRule.setContent {
            MaterialTheme {
                Box(Modifier.requiredWidth(width)) {
                    PlayerInteractionRow(
                        isFavorited = false,
                        isInWatchLater = false,
                        shareUrl = "https://www.youtube.com/watch?v=8E-cXrEgz2U",
                        onToggleFavorite = {},
                        onToggleWatchLater = {},
                        onAddToPlaylist = {},
                        onShowComments = onComments,
                        onDownload = {},
                        audioOnlyAvailable = true,
                    )
                }
            }
        }
    }
}
