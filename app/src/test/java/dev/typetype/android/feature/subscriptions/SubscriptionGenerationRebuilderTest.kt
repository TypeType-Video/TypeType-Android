package dev.typetype.android.feature.subscriptions

import dev.typetype.android.domain.feed.SubscriptionsPage
import dev.typetype.android.domain.feed.Video
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SubscriptionGenerationRebuilderTest {
    @Test
    fun `rebuilds enough of the new generation to retain loaded depth`() = runBlocking {
        val requestedCursors = mutableListOf<String>()
        val result = rebuildSubscriptionGeneration(
            firstPage = page(ids = 1..2, nextCursor = "cursor-2"),
            targetVideoCount = 5,
        ) { cursor, generation ->
            requestedCursors += cursor
            assertEquals(2L, generation)
            Result.success(
                when (cursor) {
                    "cursor-2" -> page(ids = 3..4, nextCursor = "cursor-3")
                    else -> page(ids = 5..6, nextCursor = "cursor-4")
                },
            )
        }

        assertEquals(listOf("cursor-2", "cursor-3"), requestedCursors)
        assertEquals(listOf("1", "2", "3", "4", "5", "6"), result.getOrThrow().videos.map { it.id })
        assertEquals("cursor-4", result.getOrThrow().nextCursor)
    }

    @Test
    fun `deduplicates pages without mixing generations`() = runBlocking {
        val result = rebuildSubscriptionGeneration(
            firstPage = page(ids = 1..2, nextCursor = "next"),
            targetVideoCount = 3,
        ) { _, _ ->
            Result.success(page(ids = 2..3, nextCursor = null))
        }

        assertEquals(listOf("1", "2", "3"), result.getOrThrow().videos.map { it.id })
        assertEquals(null, result.getOrThrow().nextCursor)
    }

    @Test
    fun `rejects a continuation from another generation`() = runBlocking {
        val result = rebuildSubscriptionGeneration(
            firstPage = page(ids = 1..2, nextCursor = "next"),
            targetVideoCount = 3,
        ) { _, _ ->
            Result.success(page(ids = 3..4, nextCursor = null, generation = 3L))
        }

        assertTrue(result.isFailure)
    }

    private fun page(
        ids: IntRange,
        nextCursor: String?,
        generation: Long = 2L,
    ) = SubscriptionsPage(
        videos = ids.map(::video),
        nextCursor = nextCursor,
        generation = generation,
        generatedAtMillis = 100L,
        refreshing = false,
    )

    private fun video(id: Int) = Video(
        id = id.toString(),
        url = "video-$id",
        title = "Video $id",
        thumbnailUrl = "",
        uploaderName = "Channel",
        uploaderUrl = "channel",
        uploaderAvatarUrl = "",
        uploaderVerified = false,
        durationSeconds = 60L,
        isLive = false,
        viewCount = 0L,
        uploadedAtMillis = 1L,
        isShortFormContent = false,
        shortDescription = null,
    )
}
