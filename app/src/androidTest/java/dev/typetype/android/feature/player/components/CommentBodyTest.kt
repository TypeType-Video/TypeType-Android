package dev.typetype.android.feature.player.components

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.typetype.android.domain.comments.Comment
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CommentBodyTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun longCommentCollapsesAndExpandsWithReadMore() {
        val suffix = " more comment detail".repeat(60)
        val comment = Comment(
            id = "comment",
            text = "Long comment starts here$suffix",
            authorName = "Reporter",
            authorAvatarUrl = "",
            likeCount = 2,
            textualLikeCount = "2",
            publishedTime = "1 day ago",
            isHeartedByUploader = false,
            isPinned = false,
            uploaderVerified = false,
            replyCount = 0,
        )

        composeRule.setContent {
            CommentBody(
                comment = comment,
                avatarSize = 36.dp,
                onUrlClick = {},
                onTimestampClick = {},
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            )
        }

        composeRule.onNodeWithText("Read more").assertExists().performClick()
        composeRule.onNodeWithText("Show less").assertExists().performClick()
        composeRule.onNodeWithText("Read more").assertExists()
    }
}
