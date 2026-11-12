package dev.typetype.android.feature.player.components

import dev.typetype.android.domain.comments.Comment
import dev.typetype.android.domain.comments.CommentSearchPage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CommentSearchControllerTest {
    private val calls = mutableListOf<Pair<String, String?>>()
    private val responses = ArrayDeque<Result<CommentSearchPage>>()
    private val controller = CommentSearchController(CoroutineScope(Dispatchers.Unconfined)) { query, cursor ->
        calls += query to cursor
        responses.removeFirst()
    }

    @Test
    fun submittedQueryIsTrimmedAndKeepsTheServerCursor() {
        responses += Result.success(page(listOf("a"), cursor = "next", scanned = 3))

        controller.submit("  hello world ")

        assertEquals(listOf("hello world" to null), calls)
        assertEquals(listOf("a"), controller.state.comments.map(Comment::id))
        assertEquals(3, controller.state.scannedPages)
        assertEquals("next", controller.state.nextCursor)
        assertFalse(controller.state.isLoading)
    }

    @Test
    fun searchingFurtherAppendsNewMatchesAndCountsAllScannedPages() {
        responses += Result.success(page(listOf("a", "b"), cursor = "next", scanned = 2))
        responses += Result.success(page(listOf("b", "c"), cursor = null, scanned = 4))

        controller.submit("cat")
        controller.searchFurther()

        assertEquals(listOf("cat" to null, "cat" to "next"), calls)
        assertEquals(listOf("a", "b", "c"), controller.state.comments.map(Comment::id))
        assertEquals(6, controller.state.scannedPages)
        assertNull(controller.state.nextCursor)
    }

    @Test
    fun retryAfterAFailureAsksForTheSamePageAgain() {
        responses += Result.success(page(listOf("a"), cursor = "next", scanned = 1))
        responses += Result.failure(IllegalStateException("offline"))
        responses += Result.success(page(listOf("b"), cursor = null, scanned = 1))

        controller.submit("cat")
        controller.searchFurther()
        assertTrue(controller.state.failed)
        controller.retry()

        assertEquals(listOf("cat" to null, "cat" to "next", "cat" to "next"), calls)
        assertEquals(listOf("a", "b"), controller.state.comments.map(Comment::id))
        assertFalse(controller.state.failed)
    }

    @Test
    fun resubmittingTheSameQueryDoesNotSearchAgainAndBlankClears() {
        responses += Result.success(page(listOf("a"), cursor = null, scanned = 1))

        controller.submit("cat")
        controller.submit("cat ")
        controller.submit("   ")

        assertEquals(1, calls.size)
        assertEquals("", controller.state.query)
        assertTrue(controller.state.comments.isEmpty())
    }

    private fun page(ids: List<String>, cursor: String?, scanned: Int) = CommentSearchPage(
        comments = ids.map(::comment),
        nextCursor = cursor,
        scannedPages = scanned,
        commentsDisabled = false,
    )

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
