package dev.typetype.android.playback

import dev.typetype.android.playback.RECOMMENDED_QUALITY_KEY
import dev.typetype.android.playback.SabrFixtures.SupportedCodecs
import dev.typetype.android.playback.SabrFixtures.audio
import dev.typetype.android.playback.SabrFixtures.binding
import dev.typetype.android.playback.SabrFixtures.preparedSession
import dev.typetype.android.playback.SabrFixtures.requestKey
import dev.typetype.android.playback.SabrFixtures.sabrStream
import dev.typetype.android.playback.SabrFixtures.target
import dev.typetype.android.playback.SabrFixtures.video
import dev.typetype.android.playback.initialQuality
import dev.typetype.android.playback.pickPlayableSource
import dev.typetype.android.playback.pickSabrSource
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
}
