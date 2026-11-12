package dev.typetype.android.feature.player

import dev.typetype.android.domain.feed.Video
import dev.typetype.android.domain.stream.Stream
import dev.typetype.android.domain.stream.StreamAudioSource
import dev.typetype.android.domain.stream.StreamPlaybackContract
import dev.typetype.android.domain.stream.StreamRequestScope
import dev.typetype.android.domain.stream.StreamSubtitleSource
import dev.typetype.android.domain.stream.StreamVideoSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class ProgressiveStreamMetadataTest {
    @Test
    fun enrichedDetailsDoNotReplacePlaybackContractOrFormats() {
        val bootstrap = stream(title = "Bootstrap")
        val related = listOf(video("related"))
        val details = stream(title = "Detailed").copy(
            description = "Full description",
            uploaderSubscriberCount = 42,
            likeCount = 12,
            relatedStreams = related,
            sabrVideoStreams = listOf(videoSource(itag = 399)),
            sabrAudioStreams = listOf(audioSource(itag = 251)),
        )

        val merged = bootstrap.withMetadataFrom(details)

        assertEquals("Detailed", merged.title)
        assertEquals("Full description", merged.description)
        assertEquals(42, merged.uploaderSubscriberCount)
        assertEquals(12, merged.likeCount)
        assertEquals(related, merged.relatedStreams)
        assertSame(bootstrap.sabrVideoStreams, merged.sabrVideoStreams)
        assertSame(bootstrap.sabrAudioStreams, merged.sabrAudioStreams)
        assertSame(bootstrap.requestScope, merged.requestScope)
    }

    @Test
    fun incompleteDetailsKeepUsefulBootstrapMetadata() {
        val bootstrap = stream(title = "Bootstrap").copy(uploaderVerified = true)
        val details = stream(title = "").copy(
            uploaderName = "",
            thumbnailUrl = "",
            durationSeconds = 0,
            viewCount = -1,
            likeCount = -1,
        )

        val merged = bootstrap.withMetadataFrom(details)

        assertEquals(bootstrap.title, merged.title)
        assertEquals(bootstrap.uploaderName, merged.uploaderName)
        assertEquals(bootstrap.thumbnailUrl, merged.thumbnailUrl)
        assertEquals(bootstrap.durationSeconds, merged.durationSeconds)
        assertEquals(bootstrap.viewCount, merged.viewCount)
        assertEquals(bootstrap.likeCount, merged.likeCount)
        assertEquals(true, merged.uploaderVerified)
    }

    @Test
    fun bootstrapSubtitlesAreNotReplacedByGenericMetadataUrls() {
        val sessionSubtitle = subtitle(
            id = "en-manual",
            url = "https://video.example/api/subtitles/en.vtt",
        )
        val genericSubtitle = subtitle(
            id = null,
            url = "https://video.example/api/proxy?url=upstream",
        )
        val bootstrap = stream(title = "Bootstrap").copy(subtitles = listOf(sessionSubtitle))
        val details = stream(title = "Detailed").copy(subtitles = listOf(genericSubtitle))

        val merged = bootstrap.withMetadataFrom(details)

        assertEquals(listOf(sessionSubtitle), merged.subtitles)
    }

    private fun stream(title: String): Stream {
        val video = listOf(videoSource(itag = 137))
        val audio = listOf(audioSource(itag = 140))
        return Stream(
            playbackContract = StreamPlaybackContract.ServerSabr,
            id = "video",
            title = title,
            uploaderName = "Channel",
            uploaderAvatarUrl = "https://images.example/avatar",
            uploaderUrl = "https://youtube.com/channel/channel",
            uploaderSubscriberCount = -1,
            uploaderVerified = false,
            thumbnailUrl = "https://images.example/thumbnail",
            description = "Bootstrap description",
            durationSeconds = 120,
            viewCount = 10,
            likeCount = -1,
            dislikeCount = -1,
            uploadedAtMillis = -1,
            hlsUrl = null,
            dashMpdUrl = null,
            progressiveUrl = null,
            serverDashManifestUrl = null,
            serverHlsManifestUrl = null,
            sabrVideoStreams = video,
            sabrAudioStreams = audio,
            requestScope = StreamRequestScope("server", "account", "https://video.example/api/"),
            startPositionMillis = 0,
        )
    }

    private fun videoSource(itag: Int) = StreamVideoSource(
        url = "https://video.example/api/sabr/manifest/video",
        mimeType = "video/mp4",
        codec = "avc1.640028",
        resolution = "1080p",
        width = 1920,
        height = 1080,
        fps = 30,
        bitrate = 3_000_000,
        isVideoOnly = true,
        itag = itag,
    )

    private fun audioSource(itag: Int) = StreamAudioSource(
        url = "https://video.example/api/sabr/manifest/video",
        mimeType = "audio/mp4",
        codec = "mp4a.40.2",
        bitrate = 128_000,
        quality = "AUDIO_QUALITY_MEDIUM",
        audioTrackId = null,
        audioTrackName = null,
        audioLocale = null,
        isOriginal = true,
        itag = itag,
    )

    private fun subtitle(id: String?, url: String) = StreamSubtitleSource(
        url = url,
        mimeType = "text/vtt",
        languageTag = "en",
        displayLanguageName = "English",
        isAutoGenerated = false,
        trackId = id,
    )

    private fun video(id: String) = Video(
        id = id,
        url = "https://youtube.com/watch?v=$id",
        title = "Related",
        uploaderName = "Channel",
        uploaderUrl = "https://youtube.com/channel/channel",
        uploaderAvatarUrl = "",
        uploaderVerified = false,
        thumbnailUrl = "",
        durationSeconds = 60,
        isLive = false,
        viewCount = 1,
        uploadedAtMillis = 1,
        isShortFormContent = false,
        shortDescription = null,
    )
}
