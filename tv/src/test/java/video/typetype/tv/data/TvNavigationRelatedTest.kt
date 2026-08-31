package video.typetype.tv.data

import org.junit.Assert.assertEquals
import org.junit.Test
import video.typetype.sdk.core.ServiceId
import video.typetype.sdk.core.Video
import video.typetype.sdk.core.VideoId

public class TvNavigationRelatedTest {
    @Test
    public fun recommendationsRemoveCurrentAndDuplicateVideos(): Unit {
        val current = video("current")
        val related = listOf(video("current"), video("next"), video("next"), video("later"))

        assertEquals(listOf("next", "later"), related.navigationRelated(current).map { it.id.value })
    }

    @Test
    public fun recommendationsAreLimitedForTvQueue(): Unit {
        val videos = (0..24).map { video("video-$it") }

        assertEquals(20, videos.navigationRelated(video("outside")).size)
    }

    private fun video(id: String): Video = Video(
        id = VideoId(id),
        title = id,
        url = "https://video/$id",
        thumbnailUrl = "",
        uploaderName = "Channel",
        uploaderUrl = "",
        uploaderAvatarUrl = "",
        durationSeconds = 60,
        viewCount = 0,
        uploadDate = "",
        uploadedAtEpochSeconds = 0,
        streamType = "video",
        isLive = false,
        isPostLive = false,
        isLiveContent = false,
        isShortFormContent = false,
        uploaderVerified = false,
        shortDescription = null,
        publishedAtEpochSeconds = null,
        requiresMembership = false,
        serviceId = ServiceId.YOUTUBE,
    )
}
