package dev.typetype.android.feature.subscriptions

import dev.typetype.android.domain.feed.Video
import org.junit.Assert.assertEquals
import org.junit.Test

class SubscriptionsRefreshContinuityTest {
    @Test
    fun refreshKeepsLoadedVideosOutsideTheFirstPage() {
        val firstPage = listOf(video("new"), video("kept-one"))
        val loadedVideos = listOf(video("kept-one"), video("kept-two"))

        val visibleVideos = mergeSubscriptionFirstPage(loadedVideos, firstPage)

        assertEquals(listOf("new", "kept-one", "kept-two"), visibleVideos.map { it.id })
    }

    private fun video(id: String) = Video(
        id = id,
        url = "https://video.example/$id",
        title = id,
        thumbnailUrl = "",
        uploaderName = "Channel",
        uploaderUrl = "https://video.example/channel",
        uploaderAvatarUrl = "",
        uploaderVerified = false,
        durationSeconds = 60L,
        isLive = false,
        viewCount = 1L,
        uploadedAtMillis = 1L,
        isShortFormContent = false,
        shortDescription = null,
    )
}
