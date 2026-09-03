package video.typetype.tv.data

import org.junit.Assert.assertEquals
import org.junit.Test
import video.typetype.sdk.core.ServiceId
import video.typetype.sdk.core.UserSettings
import video.typetype.sdk.core.Video
import video.typetype.sdk.core.VideoId

public class TvHomeContentTest {
    @Test
    public fun homeContentKeepsOnlyPlayableVodItems(): Unit {
        val videos = listOf(
            video("vod"),
            video("live", isLive = true),
            video("archive", isLiveContent = true),
            video("short", isShort = true),
            video("missing-duration", durationSeconds = 0),
        )

        assertEquals(listOf("vod"), videos.vodVisibleWith(UserSettings()).map { it.id.value })
    }

    private fun video(
        id: String,
        durationSeconds: Long = 60,
        isLive: Boolean = false,
        isLiveContent: Boolean = false,
        isShort: Boolean = false,
    ): Video = Video(
        id = VideoId(id),
        title = id,
        url = "https://video/$id",
        thumbnailUrl = "",
        uploaderName = "Channel",
        uploaderUrl = "",
        uploaderAvatarUrl = "",
        durationSeconds = durationSeconds,
        viewCount = 0,
        uploadDate = "",
        uploadedAtEpochSeconds = 0,
        streamType = "video",
        isLive = isLive,
        isPostLive = false,
        isLiveContent = isLiveContent,
        isShortFormContent = isShort,
        uploaderVerified = false,
        shortDescription = null,
        publishedAtEpochSeconds = null,
        requiresMembership = false,
        serviceId = ServiceId.YOUTUBE,
    )
}
