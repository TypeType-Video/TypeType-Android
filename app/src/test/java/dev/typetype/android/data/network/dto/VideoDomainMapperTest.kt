package dev.typetype.android.data.network.dto

import org.junit.Assert.assertTrue
import org.junit.Test

class VideoDomainMapperTest {
    @Test
    fun `post live stream type remains a replay when the boolean is absent`() {
        val video = videoItem(streamType = "post_live_stream").toDomainVideo()

        assertTrue(video.isPostLive)
        assertTrue(video.isLiveContent)
    }

    @Test
    fun `post live audio stream type remains a replay when the boolean is absent`() {
        val video = videoItem(streamType = "post_live_audio_stream").toDomainVideo()

        assertTrue(video.isPostLive)
    }

    private fun videoItem(streamType: String) = VideoItem(
        id = "video",
        title = "Replay",
        url = "https://www.youtube.com/watch?v=video",
        thumbnailUrl = "",
        uploaderName = "Channel",
        uploaderUrl = "",
        uploaderAvatarUrl = "",
        duration = 60L,
        viewCount = 1L,
        uploadDate = "",
        uploaded = 1L,
        streamType = streamType,
        isShortFormContent = false,
        uploaderVerified = false,
        isLiveContent = true,
    )
}
