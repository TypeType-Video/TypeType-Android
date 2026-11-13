package dev.typetype.android.data.channel

import dev.typetype.android.data.network.dto.ChannelPostsResponse
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChannelPostsMappingTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `server posts keep their media, poll and shared post`() {
        val body = """
            {"channelName":"C","channelUrl":"u","nextpage":"next","posts":[{"id":"a","url":"p","type":"poll",
            "text":"Vote","authorName":"C","authorUrl":"u","authorAvatarUrl":"av","publishedTime":"1 day ago",
            "publishedAt":1794492590841,"likeCount":162,"textualLikeCount":"","commentCount":1,
            "textualCommentCount":"1","images":["i1",""],"poll":{"type":"text","choices":[{"text":"Yes"},
            {"text":"No","imageUrl":""}],"textualVoteCount":"12 votes"},"video":{"url":"","title":"x",
            "thumbnailUrl":""},"sharedPost":{"id":"b","url":"q","type":"text","text":"Inner","authorName":"D",
            "authorUrl":"","authorAvatarUrl":"","publishedTime":"","textualLikeCount":"","textualCommentCount":""}}]}
        """.trimIndent()
        val page = json.decodeFromString<ChannelPostsResponse>(body)
        val post = page.posts.single().toDomain()

        assertEquals("next", page.nextpage)
        assertEquals("162", post.likeLabel)
        assertEquals("1", post.commentLabel)
        assertEquals(listOf("i1"), post.images)
        assertEquals(listOf("Yes", "No"), post.poll?.choices?.map { it.text })
        assertNull(post.poll?.choices?.last()?.imageUrl)
        assertEquals("12 votes", post.poll?.voteLabel)
        assertNull(post.video)
        assertEquals("Inner", post.sharedPost?.text)
    }
}
