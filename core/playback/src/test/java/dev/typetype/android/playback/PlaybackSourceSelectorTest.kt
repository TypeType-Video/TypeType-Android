package dev.typetype.android.playback

import dev.typetype.android.domain.preferences.VP9_CODEC_KEY
import dev.typetype.android.playback.AUTO_QUALITY_KEY
import dev.typetype.android.playback.DecoderSupport
import dev.typetype.android.playback.RECOMMENDED_QUALITY_KEY
import dev.typetype.android.playback.SourceSelectionFixtures.FakeCodecSupport
import dev.typetype.android.playback.SourceSelectionFixtures.audio
import dev.typetype.android.playback.SourceSelectionFixtures.providerStream
import dev.typetype.android.playback.SourceSelectionFixtures.video
import dev.typetype.android.playback.initialQuality
import dev.typetype.android.playback.key
import dev.typetype.android.playback.pickAudio
import dev.typetype.android.playback.pickExplicitProviderSource
import dev.typetype.android.playback.pickPlayableSource
import dev.typetype.android.playback.pickVideo
import dev.typetype.android.playback.videoItagKey
import dev.typetype.android.playback.videoSelectionKey
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackSourceSelectorTest {
    @Test
    fun `automatic label is reserved for adaptive manifests`() {
        val direct = providerStream(emptyList(), emptyList())

        assertEquals(RECOMMENDED_QUALITY_KEY, direct.initialQuality())
        assertEquals(AUTO_QUALITY_KEY, direct.copy(dashMpdUrl = "manifest").initialQuality())
    }

    @Test
    fun `explicit itag selects the requested codec at the same resolution`() {
        val h264 = video("h264", height = 1080, codec = "avc1.640028", itag = 137)
        val vp9 = video("vp9", height = 1080, codec = "vp09.00.41.08", itag = 248)
        val support = FakeCodecSupport(
            video = mapOf(h264.url to DecoderSupport.Hardware, vp9.url to DecoderSupport.Hardware),
        )

        assertEquals(vp9, listOf(h264, vp9).pickVideo(videoItagKey(248), support))
    }

    @Test
    fun `explicit format key selects a provider codec without an itag`() {
        val h264 = video("h264", height = 1080, codec = "avc1.640028")
        val vp9 = video("vp9", height = 1080, codec = "vp09.00.41.08")
        val support = FakeCodecSupport(
            video = mapOf(h264.url to DecoderSupport.Hardware, vp9.url to DecoderSupport.Hardware),
        )

        assertEquals(vp9, listOf(h264, vp9).pickVideo(vp9.videoSelectionKey(), support))
    }

    @Test
    fun `explicit provider codec creates the selected merged source`() {
        val h264 = video("h264", height = 1080, codec = "avc1.640028")
        val vp9 = video("vp9", height = 1080, codec = "vp09.00.41.08")
        val audio = audio("audio", locale = "en")
        val support = FakeCodecSupport(
            video = mapOf(h264.url to DecoderSupport.Hardware, vp9.url to DecoderSupport.Hardware),
            audio = mapOf(audio.url to DecoderSupport.Hardware),
        )

        val source = providerStream(listOf(h264, vp9), listOf(audio)).pickExplicitProviderSource(
            selectedQuality = vp9.videoSelectionKey(),
            selectedAudioKey = null,
            defaultAudioLanguage = "en",
            preferOriginalLanguage = false,
            codecSupport = support,
        )

        requireNotNull(source)
        assertEquals("vp9", source.url)
        assertEquals("audio", source.audioUrl)
        assertEquals(vp9.videoSelectionKey(), source.sourceKey)
    }

    @Test
    fun `unsupported modern codec never beats supported baseline`() {
        val h264 = video("h264", height = 720, codec = "avc1.64001f")
        val av1 = video("av1", height = 1080, codec = "av01.0.08M.08")
        val support = FakeCodecSupport(
            video = mapOf(h264.url to DecoderSupport.Hardware),
        )

        assertEquals(h264, listOf(av1, h264).pickVideo("1080p", support))
    }

    @Test
    fun `recommended codec prefers hardware over the automatic quality cap`() {
        val hardware = video("hardware", height = 720, codec = "avc1.64001f")
        val software = video("software", height = 1080, codec = "vp09.00.40.08")
        val support = FakeCodecSupport(
            video = mapOf(
                hardware.url to DecoderSupport.Hardware,
                software.url to DecoderSupport.Software,
            ),
        )

        assertEquals(
            hardware,
            listOf(software, hardware).pickVideo("1080p", support),
        )
    }

    @Test
    fun `smart codec keeps target resolution before codec efficiency`() {
        val baseline = video("baseline", height = 1080, codec = "avc1.640028")
        val modern = video("modern", height = 720, codec = "av01.0.08M.08")
        val support = FakeCodecSupport(
            video = mapOf(
                baseline.url to DecoderSupport.Hardware,
                modern.url to DecoderSupport.Hardware,
            ),
        )

        assertEquals(
            baseline,
            listOf(modern, baseline).pickVideo("1080p", support),
        )
    }

    @Test
    fun `smart codec prefers hardware AV1 then VP9 then H264`() {
        val h264 = video("h264", height = 1080, codec = "avc1.640028")
        val vp9 = video("vp9", height = 1080, codec = "vp09.00.41.08")
        val av1 = video("av1", height = 1080, codec = "av01.0.08M.08")
        val support = FakeCodecSupport(
            video = listOf(h264, vp9, av1).associate { it.url to DecoderSupport.Hardware },
        )

        assertEquals(av1, listOf(h264, vp9, av1).pickVideo("1080p", support))
        assertEquals(vp9, listOf(h264, vp9).pickVideo("1080p", support))
        assertEquals(h264, listOf(h264).pickVideo("1080p", support))
    }

    @Test
    fun `smart codec never selects software AV1 over hardware baseline`() {
        val h264 = video("h264", height = 720, codec = "avc1.64001f")
        val av1 = video("av1", height = 1080, codec = "av01.0.08M.08")
        val support = FakeCodecSupport(
            video = mapOf(
                h264.url to DecoderSupport.Hardware,
                av1.url to DecoderSupport.Software,
            ),
        )

        assertEquals(h264, listOf(av1, h264).pickVideo("1080p", support))
    }

    @Test
    fun `explicit codec keeps the requested quality`() {
        val hardware = video("hardware", height = 720, codec = "vp09.00.31.08")
        val software = video("software", height = 1080, codec = "vp09.00.40.08")
        val support = FakeCodecSupport(
            video = mapOf(
                hardware.url to DecoderSupport.Hardware,
                software.url to DecoderSupport.Software,
            ),
        )

        assertEquals(
            software,
            listOf(software, hardware).pickVideo("1080p", support, VP9_CODEC_KEY),
        )
    }

    @Test
    fun `explicit codec filters candidates independently from quality`() {
        val h264 = video("h264", height = 1080, codec = "avc1.640028")
        val vp9 = video("vp9", height = 720, codec = "vp09.00.31.08")
        val support = FakeCodecSupport(
            video = mapOf(h264.url to DecoderSupport.Hardware, vp9.url to DecoderSupport.Hardware),
        )

        assertEquals(vp9, listOf(h264, vp9).pickVideo("1080p", support, VP9_CODEC_KEY))
    }

    @Test
    fun `quality cap applies after capability filtering`() {
        val low = video("low", height = 360, codec = "avc1.42001e")
        val high = video("high", height = 1080, codec = "avc1.640028")
        val support = FakeCodecSupport(
            video = mapOf(low.url to DecoderSupport.Hardware, high.url to DecoderSupport.Hardware),
        )

        assertEquals(low, listOf(high, low).pickVideo("480p", support))
    }

    @Test
    fun `unsupported selected audio falls back to supported language`() {
        val unsupported = audio("selected", locale = "en")
        val french = audio("french", locale = "fr")
        val support = FakeCodecSupport(
            audio = mapOf(french.url to DecoderSupport.Software),
        )

        val selected = listOf(unsupported, french).pickAudio(
            selectedAudioKey = unsupported.key,
            defaultAudioLanguage = "fr",
            preferOriginalLanguage = false,
            codecSupport = support,
        )

        assertEquals(french, selected)
    }

    @Test
    fun `provider video with separate audio merges without an explicit audio key`() = runBlocking {
        val video = video("video", height = 360, codec = "avc1.64001E")
            .copy(playbackMimeType = "application/vnd.apple.mpegurl")
        val audio = audio("audio", locale = "ja")
        val support = FakeCodecSupport(
            video = mapOf(video.url to DecoderSupport.Hardware),
            audio = mapOf(audio.url to DecoderSupport.Hardware),
        )

        val source = pickPlayableSource(
            stream = providerStream(listOf(video), listOf(audio)),
            selectedQuality = RECOMMENDED_QUALITY_KEY,
            selectedAudioKey = null,
            defaultAudioLanguage = "en",
            automaticQualityCap = RECOMMENDED_QUALITY_KEY,
            preferOriginalLanguage = false,
            codecSupport = support,
            prepareSabrPlayback = { _, _, _ -> null },
        )

        requireNotNull(source)
        assertEquals("video", source.url)
        assertEquals("audio", source.audioUrl)
        assertEquals("application/vnd.apple.mpegurl", source.mimeType)
        assertEquals("audio/mp4", source.audioMimeType)
    }
}
