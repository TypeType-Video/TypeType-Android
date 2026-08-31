package video.typetype.tv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import video.typetype.sdk.core.StreamDetails
import video.typetype.sdk.core.StreamSubtitle
import video.typetype.sdk.core.ServiceId
import video.typetype.sdk.core.VideoId

public class TvStandardPlaybackTest {
    @Test
    public fun liveStreamsPreferHlsAndVodStreamsPreferDash(): Unit {
        assertEquals("hls", stream(isLive = true, hls = "https://media/live.m3u8", dash = "https://media/live.mpd").standardManifest()?.protocol)
        assertEquals("dash", stream(hls = "https://media/vod.m3u8", dash = "https://media/vod.mpd").standardManifest()?.protocol)
    }

    @Test
    public fun missingManifestDoesNotCreateAStandardSession(): Unit {
        assertNull(stream().standardPlaybackSession(ServiceId.BILIBILI))
        assertNull(stream(hls = "https://media/video.m3u8").standardPlaybackSession(ServiceId.YOUTUBE))
    }

    @Test
    public fun standardSessionDoesNotExposeYoutubeSubtitleMetadata(): Unit {
        val session = stream(hls = "https://media/video.m3u8").standardPlaybackSession(ServiceId.BILIBILI)
        assertEquals("manifest-video", session?.sessionId)
        assertEquals("hls", session?.protocol)
        assertEquals(0, session?.subtitles?.size)
    }

    private fun stream(
        isLive: Boolean = false,
        hls: String? = null,
        dash: String? = null,
    ): StreamDetails = StreamDetails(
        id = VideoId("video"), title = "Video", uploaderName = "Channel", uploaderUrl = "",
        uploaderAvatarUrl = "", thumbnailUrl = "", description = "", durationSeconds = 60,
        viewCount = 0, likeCount = 0, dislikeCount = 0, uploadDate = "", uploadedAtEpochSeconds = 0,
        publishedAtEpochSeconds = null, streamType = "video", isLive = isLive, isPostLive = false,
        isLiveContent = isLive, hasLiveManifest = isLive, isShortFormContent = false,
        originalAudioTrackId = null, preferredDefaultAudioTrackId = null, videoStreams = emptyList(),
        audioStreams = emptyList(), videoOnlyStreams = emptyList(), subtitles = listOf(
            StreamSubtitle("", "text/vtt", "fr", "French", false),
        ), relatedStreams = emptyList(), hlsUrl = hls, dashMpdUrl = dash,
    )
}
