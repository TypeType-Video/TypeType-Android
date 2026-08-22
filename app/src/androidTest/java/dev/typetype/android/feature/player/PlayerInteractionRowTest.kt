package dev.typetype.android.feature.player

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import dev.typetype.android.core.ui.theme.TypeTypeTheme
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PlayerInteractionRowTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun commentsActionOpensComments() {
        val opened = AtomicBoolean(false)
        composeRule.setContent {
            TypeTypeTheme {
                PlayerInteractionRow(
                    isFavorited = false,
                    isInWatchLater = false,
                    shareUrl = "/watch?v=test",
                    onToggleFavorite = {},
                    onToggleWatchLater = {},
                    onAddToPlaylist = {},
                    onShowComments = { opened.set(true) },
                    onDownload = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Comments").performClick()

        assertTrue(opened.get())
    }

    @Test
    fun commentsActionIsHiddenWhenCommentsAreDisabled() {
        composeRule.setContent {
            TypeTypeTheme {
                PlayerInteractionRow(
                    isFavorited = false,
                    isInWatchLater = false,
                    shareUrl = "/watch?v=test",
                    onToggleFavorite = {},
                    onToggleWatchLater = {},
                    onAddToPlaylist = {},
                    onShowComments = null,
                    onDownload = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Comments").assertDoesNotExist()
    }
}
