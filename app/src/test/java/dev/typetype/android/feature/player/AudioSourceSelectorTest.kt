package dev.typetype.android.feature.player

import dev.typetype.android.domain.stream.SabrPlaybackSession
import dev.typetype.android.domain.stream.Stream
import dev.typetype.android.domain.stream.StreamAudioSource
import dev.typetype.android.domain.stream.StreamPlaybackContract
import dev.typetype.android.domain.stream.StreamRequestScope
import dev.typetype.android.domain.stream.StreamVideoSource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class AudioSourceSelectorTest {
    @Test
    fun `auto uses server default instead of the first dubbed track`() {
        val selected = issueTracks().pickAudio(
            selectedAudioKey = null,
            defaultAudioLanguage = "",
            preferOriginalLanguage = false,
            codecSupport = SupportedCodecs,
            preferredDefaultAudioTrackId = "en-US.4",
            originalAudioTrackId = "en-US.4",
        )

        assertEquals("en-US.4", selected?.audioTrackId)
    }

    @Test
    fun `configured language takes priority in auto mode`() {
        val selected = issueTracks().pickAudio(
            selectedAudioKey = null,
            defaultAudioLanguage = "fr_FR",
            preferOriginalLanguage = false,
            codecSupport = SupportedCodecs,
            preferredDefaultAudioTrackId = "en-US.4",
            originalAudioTrackId = "en-US.4",
        )

        assertEquals("fr-FR.10", selected?.audioTrackId)
    }

    @Test
    fun `original language preference overrides configured language`() {
        val selected = issueTracks().pickAudio(
            selectedAudioKey = null,
            defaultAudioLanguage = "fr",
            preferOriginalLanguage = true,
            codecSupport = SupportedCodecs,
            preferredDefaultAudioTrackId = "fr-FR.10",
            originalAudioTrackId = "en-US.4",
        )

        assertEquals("en-US.4", selected?.audioTrackId)
    }

    @Test
    fun `manual audio selection overrides automatic preferences`() {
        val selected = issueTracks().pickAudio(
            selectedAudioKey = "fr-FR.10",
            defaultAudioLanguage = "en",
            preferOriginalLanguage = true,
            codecSupport = SupportedCodecs,
            preferredDefaultAudioTrackId = "en-US.4",
            originalAudioTrackId = "en-US.4",
        )

        assertEquals("fr-FR.10", selected?.audioTrackId)
    }

    @Test
    fun `SABR preparation receives the server selected original track`() = runBlocking {
        var preparedAudioTrackId: String? = null
        val source = pickSabrSource(
            stream = issueStream(),
            selectedQuality = "1080p",
            selectedAudioKey = null,
            defaultAudioLanguage = "",
            preferOriginalLanguage = false,
            codecSupport = SupportedCodecs,
            prepareSabrPlayback = { _, selection, _ ->
                preparedAudioTrackId = selection.audio.audioTrackId
                SabrPlaybackSession(
                    sessionId = "session",
                    manifestUrl = "https://instance.example/playback/session/manifest",
                    generation = 0,
                    videoItag = selection.video.itag,
                    audioItag = selection.audio.itag,
                    audioTrackId = selection.audio.audioTrackId,
                )
            },
        )

        requireNotNull(source)
        assertEquals("en-US.4", preparedAudioTrackId)
        assertEquals("sabr:8P0JoCiSwKY:137:140:en-US.4:video", source.sourceKey)
    }

    private fun issueTracks() = listOf(
        audio(trackId = "fr-FR.10", locale = "fr-FR", original = false),
        audio(trackId = "en-US.4", locale = "en-US", original = true),
    )

    private fun issueStream() = Stream(
        playbackContract = StreamPlaybackContract.ServerSabr,
        id = "8P0JoCiSwKY",
        title = "Video",
        uploaderName = "Channel",
        uploaderAvatarUrl = "",
        uploaderUrl = "",
        uploaderSubscriberCount = 0,
        uploaderVerified = false,
        thumbnailUrl = "",
        description = "",
        durationSeconds = 60,
        viewCount = 0,
        likeCount = 0,
        dislikeCount = 0,
        uploadedAtMillis = 0,
        hlsUrl = null,
        dashMpdUrl = null,
        progressiveUrl = null,
        serverDashManifestUrl = null,
        serverHlsManifestUrl = null,
        serverSabrManifestUrl = "https://instance.example/sabr/manifest",
        sabrVideoStreams = listOf(video()),
        sabrAudioStreams = issueTracks(),
        requestScope = StreamRequestScope("server", "account", "https://instance.example/"),
        originalAudioTrackId = "en-US.4",
        preferredDefaultAudioTrackId = "en-US.4",
        startPositionMillis = 0,
    )

    private fun video() = StreamVideoSource(
        url = "https://instance.example/sabr/manifest",
        mimeType = "video/mp4",
        codec = "avc1.640028",
        resolution = "1080p",
        width = 1920,
        height = 1080,
        fps = 30,
        bitrate = 4_000_000,
        isVideoOnly = true,
        itag = 137,
    )

    private fun audio(trackId: String, locale: String, original: Boolean) = StreamAudioSource(
        url = "https://instance.example/sabr/manifest",
        mimeType = "audio/mp4",
        codec = "mp4a.40.2",
        bitrate = 128_000,
        quality = "medium",
        audioTrackId = trackId,
        audioTrackName = locale,
        audioLocale = locale,
        isOriginal = original,
        itag = 140,
    )

    private object SupportedCodecs : PlaybackCodecSupport {
        override fun video(source: StreamVideoSource) = DecoderSupport.Hardware
        override fun audio(source: StreamAudioSource) = DecoderSupport.Hardware
    }
}
