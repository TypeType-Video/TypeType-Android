package video.typetype.tv.data

import org.junit.Assert.assertEquals
import org.junit.Test
import video.typetype.sdk.core.StreamAudio
import video.typetype.sdk.core.StreamDetails
import video.typetype.sdk.core.StreamVideo
import video.typetype.sdk.core.VideoId
import video.typetype.sdk.core.UserSettings
import video.typetype.sdk.core.selectSabrPlaybackTracks

public class PlaybackSelectionTest {
    @Test
    public fun defaultAudioUsesTheOriginalServerTrack(): Unit {
        val dubbed = audio(itag = 251, trackId = "fr", name = "French", bitrate = 160_000)
        val originalLow = audio(itag = 250, trackId = "en", name = "English", bitrate = 96_000, original = true)
        val originalHigh = audio(itag = 140, trackId = "en", name = "English", bitrate = 128_000, original = true)
        val stream = stream(listOf(dubbed, originalLow, originalHigh), originalTrackId = "en")

        assertEquals(140, stream.selectSabrPlaybackTracks()?.audio?.itag)
        assertEquals(251, stream.selectSabrPlaybackTracks(preferredAudioItag = 251)?.audio?.itag)
        assertEquals(
            "en",
            stream.selectSabrPlaybackTracks(preferredAudioItag = 140, preferredAudioTrackId = "en")?.audio?.audioTrackId,
        )
    }

    @Test
    public fun tvSelectionSkipsUnsupportedCodecBeforeApplyingSmartOrder(): Unit {
        val av1 = video(itag = 400, codec = "av01.0.08M.08")
        val vp9 = video(itag = 401, codec = "vp09.00.41.08")
        val stream = stream(
            audio = listOf(audio(140, "en", "English", 128_000, original = true)),
            originalTrackId = "en",
            videos = listOf(av1, vp9, video()),
        )

        assertEquals(400, stream.selectTvPlaybackTracks(isVideoSupported = { true })?.video?.itag)
        val selected = stream.selectTvPlaybackTracks(
            isVideoSupported = { it.itag != 400 },
        )

        assertEquals(401, selected?.video?.itag)
    }

    @Test
    public fun tvAudioPreferenceFollowsTheUserLanguagePolicy(): Unit {
        val stream = stream(
            audio = listOf(
                audio(140, "en", "English", 128_000, original = true),
                audio(141, "fr", "French", 128_000),
            ),
            originalTrackId = "en",
        )

        assertEquals("en", stream.defaultTvAudioTrackId(UserSettings(preferOriginalLanguage = true)))
        assertEquals(
            "fr",
            stream.defaultTvAudioTrackId(
                UserSettings(preferOriginalLanguage = false, defaultAudioLanguage = "fr-FR"),
            ),
        )
        assertEquals("fr", stream.defaultTvAudioTrackId(UserSettings(preferOriginalLanguage = false)))
    }

    private fun stream(
        audio: List<StreamAudio>,
        originalTrackId: String,
        videos: List<StreamVideo> = listOf(video()),
    ): StreamDetails = StreamDetails(
        id = VideoId("video"),
        title = "Video",
        uploaderName = "Channel",
        uploaderUrl = "/channel",
        uploaderAvatarUrl = "",
        thumbnailUrl = "",
        description = "",
        durationSeconds = 60,
        viewCount = 0,
        likeCount = 0,
        dislikeCount = 0,
        uploadDate = "",
        uploadedAtEpochSeconds = 0,
        publishedAtEpochSeconds = null,
        streamType = "video",
        isLive = false,
        isPostLive = false,
        isLiveContent = false,
        hasLiveManifest = false,
        isShortFormContent = false,
        originalAudioTrackId = originalTrackId,
        preferredDefaultAudioTrackId = "fr",
        videoStreams = emptyList(),
        audioStreams = audio,
        videoOnlyStreams = videos,
        subtitles = emptyList(),
        relatedStreams = emptyList(),
    )

    private fun video(
        itag: Int = 137,
        codec: String = "avc1.640028",
    ): StreamVideo = StreamVideo(
        url = "",
        mimeType = "video/mp4",
        format = "1080p",
        resolution = "1080p",
        bitrate = 2_000_000,
        codec = codec,
        isVideoOnly = true,
        itag = itag,
        width = 1920,
        height = 1080,
        frameRate = 30,
        contentLength = 1,
        initStart = 0,
        initEnd = 0,
        indexStart = 0,
        indexEnd = 0,
        deliveryMethod = "sabr",
        manifestUrl = null,
        sabrSessionUrl = "/session",
    )

    private fun audio(
        itag: Int,
        trackId: String,
        name: String,
        bitrate: Long,
        original: Boolean = false,
    ): StreamAudio = StreamAudio(
        url = "",
        mimeType = "audio/mp4",
        format = "audio",
        bitrate = bitrate,
        codec = "mp4a.40.2",
        quality = "medium",
        itag = itag,
        contentLength = 1,
        initStart = 0,
        initEnd = 0,
        indexStart = 0,
        indexEnd = 0,
        audioTrackId = trackId,
        audioTrackName = name,
        audioLocale = trackId,
        isOriginal = original,
        deliveryMethod = "sabr",
        manifestUrl = null,
        sabrSessionUrl = "/session",
    )
}
