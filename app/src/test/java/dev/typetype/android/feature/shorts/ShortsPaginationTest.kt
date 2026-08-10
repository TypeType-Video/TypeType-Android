package dev.typetype.android.feature.shorts

import dev.typetype.android.domain.feed.ShortsContinuation
import dev.typetype.android.domain.feed.ShortsPage
import dev.typetype.android.domain.feed.Video
import dev.typetype.android.domain.feed.shortIdentity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShortsPaginationTest {
    @Test
    fun mergeDropsCanonicalDuplicatesAcrossPages() {
        val existing = video("first", "https://youtube.com/shorts/abcdefghijk")
        val requested = ShortsContinuation.Recommendations("page-2")

        val merged = mergeShortsPage(
            knownIdentities = mutableSetOf(existing.shortIdentity()),
            requestedContinuation = requested,
            page = ShortsPage(
                videos = listOf(
                    video("duplicate", "https://www.youtube.com/watch?v=abcdefghijk"),
                    video("next", "https://youtube.com/shorts/lmnopqrstuv"),
                ),
                continuation = ShortsContinuation.Recommendations("page-3"),
            ),
        )

        assertEquals(listOf("next"), merged.additions.map(Video::id))
        assertEquals(ShortsContinuation.Recommendations("page-3"), merged.continuation)
    }

    @Test
    fun repeatedContinuationEndsPagination() {
        val requested = ShortsContinuation.Discovery("same-page")

        val merged = mergeShortsPage(
            knownIdentities = mutableSetOf(),
            requestedContinuation = requested,
            page = ShortsPage(videos = emptyList(), continuation = requested),
        )

        assertNull(merged.continuation)
    }

    private fun video(id: String, url: String) = Video(
        id = id,
        url = url,
        title = id,
        thumbnailUrl = "",
        uploaderName = "Channel",
        uploaderUrl = "https://channel/$id",
        uploaderAvatarUrl = "",
        uploaderVerified = false,
        durationSeconds = 30,
        isLive = false,
        viewCount = 1,
        uploadedAtMillis = 1,
        isShortFormContent = true,
        shortDescription = null,
    )
}
