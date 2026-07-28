package dev.typetype.android.feature.player

import dev.typetype.android.domain.stream.SabrPlaybackBinding
import dev.typetype.android.domain.stream.Stream
import dev.typetype.android.domain.stream.StreamAudioSource
import dev.typetype.android.domain.stream.StreamPlaybackContract
import dev.typetype.android.domain.stream.StreamRequestScope
import dev.typetype.android.domain.stream.StreamSubtitleSource
import dev.typetype.android.domain.stream.StreamVideoSource
import dev.typetype.android.domain.stream.SabrPlaybackSession
import dev.typetype.android.domain.stream.SabrPlaybackTarget
import dev.typetype.android.domain.stream.sabrSubtitleCatalogFingerprint
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SabrPlaybackSourceTest {
    @Test
    fun `SABR starts with a recommended device selection instead of fake auto`() {
        assertEquals(RECOMMENDED_QUALITY_KEY, sabrStream().initialQuality())
    }

    @Test
    fun `server SABR contract selects the custom playback source`() = runBlocking {
        val source = pickPlayableSource(
            stream = sabrStream(),
            selectedQuality = "1080p",
            selectedAudioKey = "en.0",
            defaultAudioLanguage = "en",
            automaticQualityCap = "1080p",
            preferOriginalLanguage = true,
            codecSupport = SupportedCodecs,
            prepareSabrPlayback = { _, _, _ -> preparedSession() },
        )

        requireNotNull(source)
        assertEquals("typetype-sabr://playback/session", source.url)
        assertEquals("application/x-typetype-sabr", source.mimeType)
        assertEquals("sabr:video:137:140:en.0:video", source.sourceKey)
        assertEquals(requestKey(sabrStream()), source.sabrRequestKey)
        assertEquals(binding(), source.sabrBinding)
        assertEquals(target(), source.sabrTarget)
        assertNull(source.audioUrl)
    }

    @Test
    fun `source identity uses the tuple accepted by the recovered session`() = runBlocking {
        val source = pickPlayableSource(
            stream = sabrStream().copy(sabrVideoStreams = listOf(video(), video(136, 720))),
            selectedQuality = "1080p",
            selectedAudioKey = "en.0",
            defaultAudioLanguage = "en",
            automaticQualityCap = "1080p",
            preferOriginalLanguage = true,
            codecSupport = SupportedCodecs,
            prepareSabrPlayback = { _, _, _ -> preparedSession(videoItag = 136) },
        )

        requireNotNull(source)
        assertEquals("sabr:video:136:140:en.0:video", source.sourceKey)
        assertEquals(requestKey(sabrStream()), source.sabrRequestKey)
        assertEquals(target(videoItag = 136), source.sabrTarget)
    }

    @Test
    fun `prepared SABR source is reused for the same requested tuple`() {
        val source = reusableSabrSource(
            requestKey = "sabr:video:137:140:en.0",
            storedRequestKey = "sabr:video:137:140:en.0",
            acceptedKey = "sabr:video:136:140:en.0:video",
            url = "typetype-sabr://playback/session",
            sabrBinding = binding(videoItag = 136),
            sabrTarget = target(videoItag = 136),
        )

        requireNotNull(source)
        assertEquals("sabr:video:136:140:en.0:video", source.sourceKey)
        assertEquals("sabr:video:137:140:en.0", source.sabrRequestKey)
    }

    @Test
    fun `prepared SABR source is not reused for another requested tuple`() {
        val source = reusableSabrSource(
            requestKey = "sabr:video:135:140:en.0",
            storedRequestKey = "sabr:video:137:140:en.0",
            acceptedKey = "sabr:video:136:140:en.0:video",
            url = "typetype-sabr://playback/session",
            sabrBinding = binding(videoItag = 136),
            sabrTarget = target(videoItag = 136),
        )

        assertNull(source)
    }

    @Test
    fun `prepared SABR source without its accepted binding is not reused`() {
        val source = reusableSabrSource(
            requestKey = "sabr:video:137:140:en.0",
            storedRequestKey = "sabr:video:137:140:en.0",
            acceptedKey = "sabr:video:137:140:en.0:video",
            url = "typetype-sabr://playback/session",
            sabrBinding = null,
            sabrTarget = target(),
        )

        assertNull(source)
    }

    @Test
    fun `prepared SABR source is not reused across accounts`() {
        val firstAccount = sabrStream()
        val secondAccount = firstAccount.copy(
            requestScope = firstAccount.requestScope?.copy(accountId = "another-account"),
        )
        val firstKey = requestKey(firstAccount)
        val secondKey = requestKey(secondAccount)

        assertFalse(firstKey == secondKey)
        assertNull(
            reusableSabrSource(
                requestKey = secondKey,
                storedRequestKey = firstKey,
                acceptedKey = "sabr:video:137:140:en.0:video",
                url = "typetype-sabr://playback/session",
                sabrBinding = binding(),
                sabrTarget = target(),
            ),
        )
    }

    @Test
    fun `fresh SABR session replaces the failed source for the same tuple`() {
        val replacement = PlayableSource(
            url = "typetype-sabr://playback/fresh",
            mimeType = "application/x-typetype-sabr",
            sourceKey = "sabr:video:137:140:en.0:video",
            sabrRequestKey = "sabr:video:137:140:en.0",
        )

        assertFalse(
            samePlayableSource(
                currentUrl = "typetype-sabr://playback/failed",
                currentSourceKey = replacement.sourceKey,
                currentAudioUrl = null,
                requestedSource = replacement,
            ),
        )
    }

    @Test
    fun `server SABR contract never falls through to classic media`() = runBlocking {
        val stream = sabrStream().copy(
            serverSabrManifestUrl = null,
            sabrVideoStreams = emptyList(),
            sabrAudioStreams = emptyList(),
        )

        val source = pickPlayableSource(
            stream = stream,
            selectedQuality = "auto",
            selectedAudioKey = null,
            defaultAudioLanguage = "",
            automaticQualityCap = "1080p",
            preferOriginalLanguage = false,
            codecSupport = SupportedCodecs,
            prepareSabrPlayback = { _, _, _ -> error("SABR preparation must not run without formats") },
        )

        assertNull(source)
    }

    @Test
    fun `server SABR contract rejects a format without an itag`() = runBlocking {
        var preparationCalled = false
        val source = pickPlayableSource(
            stream = sabrStream().copy(sabrVideoStreams = listOf(video().copy(itag = 0))),
            selectedQuality = "1080p",
            selectedAudioKey = "en.0",
            defaultAudioLanguage = "en",
            automaticQualityCap = "1080p",
            preferOriginalLanguage = true,
            codecSupport = SupportedCodecs,
            prepareSabrPlayback = { _, _, _ ->
                preparationCalled = true
                preparedSession()
            },
        )

        assertNull(source)
        assertFalse(preparationCalled)
    }

    @Test
    fun `server SABR contract rejects a format without a server source`() = runBlocking {
        var preparationCalled = false
        val source = pickPlayableSource(
            stream = sabrStream().copy(sabrVideoStreams = listOf(video().copy(url = ""))),
            selectedQuality = "1080p",
            selectedAudioKey = "en.0",
            defaultAudioLanguage = "en",
            automaticQualityCap = "1080p",
            preferOriginalLanguage = true,
            codecSupport = SupportedCodecs,
            prepareSabrPlayback = { _, _, _ ->
                preparationCalled = true
                preparedSession()
            },
        )

        assertNull(source)
        assertFalse(preparationCalled)
    }

    @Test
    fun `server SABR selection excludes formats rejected by the server`() = runBlocking {
        var acceptedVideoItag = 0
        var acceptedAudioItag = 0
        var recoveryVideoItags = emptySet<Int>()
        val source = pickSabrSource(
            stream = sabrStream().copy(
                sabrVideoStreams = listOf(
                    video(),
                    video().copy(codec = "hvc1.1.6.L120", height = 720, itag = 136),
                ),
                sabrAudioStreams = listOf(
                    audio().copy(mimeType = "audio/webm", codec = "opus", bitrate = 256_000, itag = 251),
                    audio(),
                ),
            ),
            selectedQuality = "1080p",
            selectedAudioKey = null,
            defaultAudioLanguage = "",
            preferOriginalLanguage = false,
            codecSupport = SupportedCodecs,
            prepareSabrPlayback = { _, selection, _ ->
                acceptedVideoItag = selection.video.itag
                acceptedAudioItag = selection.audio.itag
                recoveryVideoItags = selection.recoveryVideoItags
                preparedSession()
            },
        )

        requireNotNull(source)
        assertEquals(137, acceptedVideoItag)
        assertEquals(140, acceptedAudioItag)
        assertTrue(recoveryVideoItags.isEmpty())
    }

    private fun sabrStream() = Stream(
        playbackContract = StreamPlaybackContract.ServerSabr,
        id = "video",
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
        hlsUrl = "https://classic.example/live.m3u8",
        dashMpdUrl = "https://classic.example/video.mpd",
        progressiveUrl = "https://classic.example/video.mp4",
        serverDashManifestUrl = "https://instance.example/streams/manifest",
        serverHlsManifestUrl = "https://classic.example/live.m3u8",
        serverSabrManifestUrl = "https://instance.example/sabr/manifest/video",
        sabrVideoStreams = listOf(video()),
        sabrAudioStreams = listOf(audio()),
        requestScope = StreamRequestScope("server", "account", "https://instance.example/"),
        muxedVideoStreams = listOf(video().copy(url = "https://classic.example/video.mp4")),
        startPositionMillis = 0,
    )

    private fun video(itag: Int = 137, height: Int = 1080) = StreamVideoSource(
        url = "https://instance.example/sabr/manifest/video",
        mimeType = "video/mp4",
        codec = "avc1.640028",
        resolution = "${height}p",
        width = height * 16 / 9,
        height = height,
        fps = 30,
        bitrate = 4_000_000,
        isVideoOnly = true,
        itag = itag,
    )

    private fun audio() = StreamAudioSource(
        url = "https://instance.example/sabr/manifest/video",
        mimeType = "audio/mp4",
        codec = "mp4a.40.2",
        bitrate = 128_000,
        quality = "medium",
        audioTrackId = "en.0",
        audioTrackName = "English",
        audioLocale = "en",
        isOriginal = true,
        itag = 140,
    )

    private fun preparedSession(videoItag: Int = 137) = SabrPlaybackSession(
        sessionId = "session",
        manifestUrl = "https://instance.example/api/sabr/playback/session/manifest",
        generation = 0,
        videoItag = videoItag,
        audioItag = 140,
        audioTrackId = "en.0",
    )

    private fun binding(videoItag: Int = 137) = SabrPlaybackBinding(
        sessionId = "session",
        generation = 0L,
        videoItag = videoItag,
        audioItag = 140,
        audioTrackId = "en.0",
    )

    private fun target(videoItag: Int = 137) = SabrPlaybackTarget(
        videoId = "video",
        requestScope = StreamRequestScope("server", "account", "https://instance.example/"),
        videoItag = videoItag,
        audioItag = 140,
        audioTrackId = "en.0",
    )

    private fun requestKey(stream: Stream): String = requireNotNull(
        stream.sabrRequestKey(
            selectedQuality = "1080p",
            selectedAudioKey = "en.0",
            defaultAudioLanguage = "en",
            preferOriginalLanguage = true,
            codecSupport = SupportedCodecs,
        ),
    )

    private object SupportedCodecs : PlaybackCodecSupport {
        override fun video(source: StreamVideoSource) = DecoderSupport.Hardware
        override fun audio(source: StreamAudioSource) = DecoderSupport.Hardware
    }
}
