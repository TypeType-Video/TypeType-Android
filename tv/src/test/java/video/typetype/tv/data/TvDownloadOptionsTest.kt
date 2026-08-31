package video.typetype.tv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import video.typetype.sdk.core.StreamAudio
import video.typetype.sdk.core.StreamDetails
import video.typetype.sdk.core.StreamVideo
import video.typetype.sdk.core.VideoId

public class TvDownloadOptionsTest {
    @Test
    public fun downloadUsesTheWatchUrlAndOriginalAudioTrack(): Unit {
        val options = buildTvDownloadOptions(stream(), "https://www.youtube.com/watch?v=video")
        val recommendedVideo = options.single { it.kind == TvDownloadKind.VIDEO && it.recommended }
        val recommendedAudio = options.single { it.kind == TvDownloadKind.AUDIO && it.recommended }

        assertEquals("https://www.youtube.com/watch?v=video", recommendedVideo.request.url)
        assertEquals(1080, recommendedVideo.request.options.height)
        assertEquals("140", recommendedVideo.request.options.audioItag)
        assertFalse(recommendedVideo.request.options.allowQualityFallback)
        assertEquals("130 kbps", recommendedAudio.label)
        assertEquals(130_000, recommendedAudio.request.options.bitrate)
    }

    @Test
    public fun downloadPresentsEveryFormatWithoutDuplicatingItags(): Unit {
        val options = buildTvDownloadOptions(stream(), "https://youtu.be/video")

        assertEquals(3, options.count { it.kind == TvDownloadKind.VIDEO })
        assertEquals(2, options.count { it.kind == TvDownloadKind.AUDIO })
        assertTrue(options.any { it.label == "2160p 60fps" })
        assertTrue(options.any { it.label == "389 kbps" })
    }

    private fun stream(): StreamDetails = StreamDetails(
        id = VideoId("video"), title = "Video", uploaderName = "Channel", uploaderUrl = "/channel",
        uploaderAvatarUrl = "", thumbnailUrl = "", description = "", durationSeconds = 60,
        viewCount = 0, likeCount = 0, dislikeCount = 0, uploadDate = "", uploadedAtEpochSeconds = 0,
        publishedAtEpochSeconds = null, streamType = "video", isLive = false, isPostLive = false,
        isLiveContent = false, hasLiveManifest = false, isShortFormContent = false,
        originalAudioTrackId = "en", preferredDefaultAudioTrackId = "fr", videoStreams = emptyList(),
        audioStreams = listOf(audio(258, 389_000, false), audio(140, 130_000, true)),
        videoOnlyStreams = listOf(video(315, 2160, 60), video(299, 1080, 60), video(136, 720, 30)),
        subtitles = emptyList(), relatedStreams = emptyList(),
    )

    private fun video(itag: Int, height: Int, fps: Int): StreamVideo = StreamVideo(
        url = "", mimeType = "video/mp4", format = "mp4", resolution = "${height}p",
        bitrate = 2_000_000, codec = "avc1.640028", isVideoOnly = true, itag = itag,
        width = height * 16 / 9, height = height, frameRate = fps, contentLength = 100,
        initStart = 0, initEnd = 0, indexStart = 0, indexEnd = 0, deliveryMethod = "sabr",
        manifestUrl = null, sabrSessionUrl = "/session",
    )

    private fun audio(itag: Int, bitrate: Long, original: Boolean): StreamAudio = StreamAudio(
        url = "", mimeType = "audio/mp4", format = "mp4", bitrate = bitrate, codec = "mp4a.40.2",
        quality = "medium", itag = itag, contentLength = 50, initStart = 0, initEnd = 0,
        indexStart = 0, indexEnd = 0, audioTrackId = if (original) "en" else "fr",
        audioTrackName = if (original) "English" else "French", audioLocale = if (original) "en" else "fr",
        isOriginal = original, deliveryMethod = "sabr", manifestUrl = null, sabrSessionUrl = "/session",
    )
}
