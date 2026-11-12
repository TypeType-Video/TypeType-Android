package dev.typetype.android.feature.channel

import dev.typetype.android.domain.channel.Channel
import dev.typetype.android.domain.channel.ChannelPage
import dev.typetype.android.domain.channel.ChannelPlaylistsPage
import dev.typetype.android.domain.feed.Video
import dev.typetype.android.domain.search.SearchPlaylist
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChannelPagingTest {
    @Test
    fun appendingPageKeepsOrderAndRemovesOverlappingVideos() {
        val first = channel(video("one"), video("two"))
        val second = channel(video("two"), video("three"))

        val merged = first.appendPage(second)

        assertEquals(listOf("one", "two", "three"), merged.videos.map { it.id })
    }

    @Test
    fun nextCursorAcceptsAChangedOpaqueValue() {
        assertEquals("next", nextChannelCursor("current", "next"))
    }

    @Test
    fun nextCursorStopsAtEndOrRepeatedCursor() {
        assertNull(nextChannelCursor("current", null))
        assertNull(nextChannelCursor("current", "current"))
    }

    @Test
    fun pageStateMergesResultsAndStopsAtTheServerEnd() {
        val state = ChannelState(channel = channel(video("one")), nextPage = "current")
        val page = ChannelPage(channel(video("two")), nextPage = null)

        val merged = state.startPageLoad().appendPage(page, "current")

        assertEquals(listOf("one", "two"), merged.channel?.videos?.map { it.id })
        assertNull(merged.nextPage)
        assertEquals(false, merged.isLoadingMore)
        assertEquals(false, merged.loadMoreError)
    }

    @Test
    fun consecutivePagesKeepEveryVideoUntilTheServerEndsPagination() {
        val initial = ChannelState(channel = channel(video("one")), nextPage = "page-two")

        val second = initial.startPageLoad().appendPage(
            ChannelPage(channel(video("two"), video("three")), nextPage = "page-three"),
            requestedCursor = "page-two",
        )
        val third = second.startPageLoad().appendPage(
            ChannelPage(channel(video("three"), video("four")), nextPage = null),
            requestedCursor = "page-three",
        )

        assertEquals(listOf("one", "two", "three", "four"), third.channel?.videos?.map { it.id })
        assertNull(third.nextPage)
        assertEquals(false, third.isLoadingMore)
    }

    @Test
    fun failedPageKeepsContentAndMakesRetryAvailable() {
        val state = ChannelState(channel = channel(video("one")), nextPage = "current")

        val failed = state.startPageLoad().failPageLoad("Unavailable", "request-id")

        assertEquals(listOf("one"), failed.channel?.videos?.map { it.id })
        assertEquals("current", failed.nextPage)
        assertEquals(false, failed.isLoadingMore)
        assertEquals(true, failed.loadMoreError)
        assertEquals("Unavailable", failed.errorMessage)
        assertEquals("request-id", failed.errorRequestId)
    }

    @Test
    fun playlistPagesUseTheirOwnCursorAndRemoveDuplicates() {
        val state = ChannelState(
            channel = channel(),
            playlists = listOf(playlist("one"), playlist("two")),
            playlistsNextPage = "current",
            playlistsLoadingMore = true,
        )
        val page = ChannelPlaylistsPage(
            playlists = listOf(playlist("two"), playlist("three")),
            nextPage = "next",
        )

        val merged = state.appendPlaylistsPage(page, "current")

        assertEquals(listOf("one", "two", "three"), merged.playlists.map { it.id })
        assertEquals("next", merged.playlistsNextPage)
        assertEquals(false, merged.playlistsLoadingMore)
    }

    @Test
    fun initialPlaylistPageReplacesOldResultsAndMarksItLoaded() {
        val state = ChannelState(channel = channel(), playlists = listOf(playlist("old")))

        val loaded = state.finishPlaylistsLoad(
            ChannelPlaylistsPage(listOf(playlist("new"), playlist("new")), nextPage = null),
        )

        assertEquals(listOf("new"), loaded.playlists.map { it.id })
        assertEquals(true, loaded.playlistsLoaded)
        assertEquals(false, loaded.playlistsLoading)
        assertNull(loaded.playlistsNextPage)
    }

    private fun channel(vararg videos: Video) = Channel(
        name = "Channel",
        description = "",
        avatarUrl = "",
        bannerUrl = null,
        subscriberCount = 0,
        verified = false,
        videos = videos.toList(),
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

    private fun playlist(id: String) = SearchPlaylist(
        id = id,
        title = id,
        url = "https://playlist/$id",
        thumbnailUrl = "",
        uploaderName = "Channel",
        streamCount = 1,
        playlistType = "playlist",
    )
}
