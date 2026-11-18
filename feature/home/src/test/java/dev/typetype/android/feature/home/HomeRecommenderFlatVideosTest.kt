package dev.typetype.android.feature.home

import dev.typetype.android.domain.feed.HomeRecommendationRow
import dev.typetype.android.domain.feed.Video
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeRecommenderFlatVideosTest {

    @Test
    fun `grid layout keeps row order, drops duplicates and hidden videos`() {
        val state = HomeRecommenderState(
            rows = listOf(row("first", "a", "b"), row("second", "b", "c", "hidden")),
        )
        val videos = listOf(video("c"), video("a"), video("b"))

        assertEquals(listOf("a", "b", "c"), state.flatVideos(videos).map { it.url })
    }

    private fun row(id: String, vararg urls: String) = HomeRecommendationRow(
        id = id,
        title = id,
        reason = "topic",
        reasonRef = null,
        videoUrls = urls.toList(),
    )

    private fun video(url: String) = Video(
        id = url,
        url = url,
        title = url,
        thumbnailUrl = "",
        uploaderName = "",
        uploaderUrl = "",
        uploaderAvatarUrl = "",
        uploaderVerified = false,
        durationSeconds = 60,
        isLive = false,
        viewCount = 0,
        uploadedAtMillis = 0,
        isShortFormContent = false,
        shortDescription = null,
    )
}
