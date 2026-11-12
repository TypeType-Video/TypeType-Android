package dev.typetype.android.data.comments

import dev.typetype.android.data.network.dto.CommentImageDto
import dev.typetype.android.data.network.dto.CommentItem
import dev.typetype.android.data.network.dto.CommentVoiceReplyDto
import dev.typetype.android.data.network.dto.CommentsPageResponse
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CommentMappingsTest {
    @Test
    fun `published timestamp survives network mapping`() {
        val comment = item().copy(publishedAt = 1_785_000_000_000L).toDomainComment()

        assertEquals(1_785_000_000_000L, comment.publishedAtMillis)
    }

    @Test
    fun `server comment media is decoded and mapped`() {
        val body = """
            {"comments":[{"id":"a","text":"","author":"A","authorUrl":"u","authorAvatarUrl":"v","likeCount":1,
            "textualLikeCount":"1","publishedTime":"now","isHeartedByUploader":false,"isPinned":false,
            "uploaderVerified":false,"replyCount":0,"image":{"url":"https://i.ytimg.com/c_exp/a.webp","animated":true},
            "voiceReply":{"url":"https://www.youtube.com/watch?v=m1","durationMs":12678,"transcript":"Hi",
            "playable":true}}]}
        """.trimIndent()
        val comment = Json { ignoreUnknownKeys = true }
            .decodeFromString<CommentsPageResponse>(body).comments.single().toDomainComment()

        assertEquals("https://i.ytimg.com/c_exp/a.webp", comment.image?.url)
        assertTrue(comment.image?.animated == true)
        assertEquals(12_678L, comment.voiceReply?.durationMs)
        assertTrue(comment.voiceReply?.playable == true)
    }

    @Test
    fun `media without content is dropped and unplayable replies keep their reason`() {
        val comment = item().copy(
            image = CommentImageDto(url = " ", unavailableText = ""),
            voiceReply = CommentVoiceReplyDto(
                url = "",
                transcript = "Only text",
                playable = true,
                unavailableText = "Not available",
            ),
        ).toDomainComment()

        assertNull(comment.image)
        assertFalse(comment.voiceReply?.playable == true)
        assertEquals("Not available", comment.voiceReply?.unavailableText)
    }

    @Test
    fun `missing image keeps the reason youtube gives`() {
        val comment = item().copy(image = CommentImageDto(url = null, unavailableText = "Image removed"))
            .toDomainComment()

        assertNull(comment.image?.url)
        assertEquals("Image removed", comment.image?.unavailableText)
    }

    private fun item() = CommentItem(
        id = "comment",
        text = "Hello",
        author = "Author",
        authorUrl = "author",
        authorAvatarUrl = "avatar",
        likeCount = 4,
        textualLikeCount = "4",
        publishedTime = "technical fallback",
        isHeartedByUploader = false,
        isPinned = false,
        uploaderVerified = false,
        replyCount = 0,
    )
}
