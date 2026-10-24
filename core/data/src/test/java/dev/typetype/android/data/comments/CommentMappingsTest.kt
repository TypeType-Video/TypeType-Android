package dev.typetype.android.data.comments

import dev.typetype.android.data.network.dto.CommentItem
import org.junit.Assert.assertEquals
import org.junit.Test

class CommentMappingsTest {
    @Test
    fun `published timestamp survives network mapping`() {
        val comment = CommentItem(
            id = "comment",
            text = "Hello",
            author = "Author",
            authorUrl = "author",
            authorAvatarUrl = "avatar",
            likeCount = 4,
            textualLikeCount = "4",
            publishedTime = "technical fallback",
            publishedAt = 1_785_000_000_000L,
            isHeartedByUploader = false,
            isPinned = false,
            uploaderVerified = false,
            replyCount = 0,
        ).toDomainComment()

        assertEquals(1_785_000_000_000L, comment.publishedAtMillis)
    }
}
