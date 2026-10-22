package dev.typetype.android.feature.player.components

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import dev.typetype.android.R
import org.junit.Rule
import org.junit.Test

class DescriptionSectionCountTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun unknownViewCountIsHidden() {
        setDescription(viewCount = -1)

        composeRule.onNodeWithText("-1").assertDoesNotExist()
        composeRule.onNodeWithContentDescription(viewsLabel("-1")).assertDoesNotExist()
    }

    @Test
    fun zeroViewCountRemainsVisible() {
        setDescription(viewCount = 0)

        composeRule.onNodeWithText("0").assertIsDisplayed()
        composeRule.onNodeWithContentDescription(viewsLabel("0")).assertIsDisplayed()
    }

    private fun setDescription(viewCount: Long) {
        composeRule.setContent {
            MaterialTheme {
                DescriptionSection(
                    title = "Test video",
                    viewCount = viewCount,
                    likeCount = 0,
                    releaseDateMillis = 0,
                    description = "",
                    videoUrl = "https://www.youtube.com/watch?v=test",
                    onTimestampClick = {},
                )
            }
        }
    }

    private fun viewsLabel(count: String): String =
        composeRule.activity.getString(R.string.player_views_count, count)
}
