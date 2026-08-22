package dev.typetype.android.feature.player.components

import dev.typetype.android.domain.comments.Comment
import org.junit.Assert.assertEquals
import org.junit.Test

class CommentRepliesTest {
    @Test
    fun continuationAppendsOnlyNewRepliesInOrder() {
        val merged = mergeCommentReplies(
            current = listOf(comment("one"), comment("two")),
            additions = listOf(comment("two"), comment("three")),
        )

        assertEquals(listOf("one", "two", "three"), merged.map(Comment::id))
    }

    private fun comment(id: String) = Comment(
        id = id,
        text = id,
        authorName = "Author",
        authorAvatarUrl = "",
        likeCount = 0,
        textualLikeCount = "",
        publishedTime = "now",
        isHeartedByUploader = false,
        isPinned = false,
        uploaderVerified = false,
        replyCount = 0,
    )
}
