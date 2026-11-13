package dev.typetype.android.feature.player.components

import dev.typetype.android.domain.comments.Comment
import dev.typetype.android.domain.comments.CommentsPage
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test

class CommentRepliesControllerTest {
    @Test
    fun `failed replies explain their own cause`() {
        val offline = IOException("offline")
        val controller = CommentRepliesController(
            scope = CoroutineScope(Dispatchers.Unconfined),
            failureMessage = { failure -> if (failure === offline) "You're offline" else "Replies failed" },
            loadReplies = { Result.failure<CommentsPage>(offline) },
        )
        val comment = Comment(
            id = "c",
            text = "c",
            authorName = "Author",
            authorAvatarUrl = "",
            likeCount = 0,
            textualLikeCount = "",
            publishedTime = "now",
            isHeartedByUploader = false,
            isPinned = false,
            uploaderVerified = false,
            replyCount = 2,
            repliesPage = "page",
        )

        controller.toggle(comment)

        assertEquals(RepliesState.Failed("You're offline"), controller.stateOf(comment))
    }
}
