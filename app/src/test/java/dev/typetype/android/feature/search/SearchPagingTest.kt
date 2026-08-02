package dev.typetype.android.feature.search

import dev.typetype.android.domain.feed.Video
import dev.typetype.android.domain.search.SearchPage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SearchPagingTest {
    @Test
    fun nonEmptyPageAcceptsChangedOpaqueCursor() {
        assertEquals("next", page(video("one"), nextPage = "next").nextSearchCursor("current"))
    }

    @Test
    fun repeatedCursorStopsPagination() {
        assertNull(page(video("one"), nextPage = "current").nextSearchCursor("current"))
    }

    @Test
    fun emptyPageStopsPaginationEvenWhenServerReturnsCursor() {
        assertNull(page(nextPage = "next").nextSearchCursor("current"))
    }

    private fun page(vararg videos: Video, nextPage: String) = SearchPage(
        videos = videos.toList(),
        channels = emptyList(),
        playlists = emptyList(),
        nextPage = nextPage,
        suggestion = null,
        isCorrected = false,
    )

    private fun video(id: String) = Video(
        id = id,
        url = "https://video/$id",
        title = id,
        thumbnailUrl = "",
        uploaderName = "Channel",
        uploaderUrl = "https://channel",
        uploaderAvatarUrl = "",
        uploaderVerified = false,
        durationSeconds = 1,
        isLive = false,
        viewCount = 0,
        uploadedAtMillis = 0,
        isShortFormContent = false,
        shortDescription = null,
    )
}
