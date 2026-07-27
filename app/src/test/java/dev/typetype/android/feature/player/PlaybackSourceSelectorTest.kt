package dev.typetype.android.feature.player

import dev.typetype.android.domain.stream.StreamAudioSource
import dev.typetype.android.domain.stream.Stream
import dev.typetype.android.domain.stream.StreamPlaybackContract
import dev.typetype.android.domain.stream.StreamVideoSource
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
    fun `explicit quality is honored before decoder class`() {
        val hardware = video("hardware", height = 720, codec = "avc1.64001f")
        val software = video("software", height = 1080, codec = "vp09.00.40.08")
        val support = FakeCodecSupport(
            video = mapOf(
                hardware.url to DecoderSupport.Hardware,
                software.url to DecoderSupport.Software,
            ),
        )

        assertEquals(software, listOf(software, hardware).pickVideo("1080p", support))
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
    fun `SABR recovery exposes only lower device playable itags`() {
        val selected = video("selected", height = 720, codec = "avc1.64001f", itag = 136)
        val lower = video("lower", height = 360, codec = "avc1.42001e", itag = 134)
        val higher = video("higher", height = 1080, codec = "avc1.640028", itag = 137)
        val unsupported = video("unsupported", height = 240, codec = "av01.0.08M.08", itag = 399)
        val support = FakeCodecSupport(
            video = mapOf(
                selected.url to DecoderSupport.Hardware,
                lower.url to DecoderSupport.Hardware,
                higher.url to DecoderSupport.Hardware,
            ),
        )

        assertEquals(
            setOf(134),
            listOf(unsupported, higher, selected, lower).playableLowerVideoItags(selected, support),
        )
    }

    @Test
    fun `SABR recovery keeps an explicitly selected codec`() {
        val selected = video("selected", height = 1080, codec = "vp09.00.41.08", itag = 248)
        val lowerVp9 = video("lower-vp9", height = 720, codec = "vp09.00.31.08", itag = 247)
        val lowerH264 = video("lower-h264", height = 720, codec = "avc1.64001f", itag = 136)
        val support = FakeCodecSupport(
            video = mapOf(
                selected.url to DecoderSupport.Hardware,
                lowerVp9.url to DecoderSupport.Hardware,
                lowerH264.url to DecoderSupport.Hardware,
            ),
        )

        assertEquals(
            setOf(247),
            listOf(lowerH264, selected, lowerVp9).playableLowerVideoItags(
                selected,
                support,
                VP9_CODEC_KEY,
            ),
        )
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

    private fun video(url: String, height: Int, codec: String, itag: Int = 0) = StreamVideoSource(
        url = url,
        mimeType = "video/mp4",
        codec = codec,
        resolution = "${height}p",
        width = height * 16 / 9,
        height = height,
        fps = 30,
        bitrate = height * 1_000,
        isVideoOnly = true,
        itag = itag,
    )

    private fun audio(url: String, locale: String) = StreamAudioSource(
        url = url,
        mimeType = "audio/mp4",
        codec = "mp4a.40.2",
        bitrate = 128_000,
        quality = "medium",
        audioTrackId = url,
        audioTrackName = locale,
        audioLocale = locale,
        isOriginal = false,
    )

    private fun providerStream(
        videos: List<StreamVideoSource>,
        audios: List<StreamAudioSource>,
    ) = Stream(
        playbackContract = StreamPlaybackContract.ProviderMedia,
        id = "video",
        title = "Video",
        uploaderName = "Channel",
        uploaderAvatarUrl = "avatar",
        uploaderUrl = "channel",
        uploaderSubscriberCount = 1,
        uploaderVerified = false,
        thumbnailUrl = "thumb",
        description = "",
        durationSeconds = 60,
        viewCount = 1,
        likeCount = 1,
        dislikeCount = 0,
        uploadedAtMillis = 1,
        hlsUrl = null,
        dashMpdUrl = null,
        progressiveUrl = null,
        serverDashManifestUrl = null,
        serverHlsManifestUrl = null,
        videoOnlyStreams = videos,
        audioStreams = audios,
        startPositionMillis = 0,
    )

    private class FakeCodecSupport(
        private val video: Map<String, DecoderSupport> = emptyMap(),
        private val audio: Map<String, DecoderSupport> = emptyMap(),
    ) : PlaybackCodecSupport {
        override fun video(source: StreamVideoSource): DecoderSupport =
            video[source.url] ?: DecoderSupport.Unsupported

        override fun audio(source: StreamAudioSource): DecoderSupport =
            audio[source.url] ?: DecoderSupport.Unsupported
    }
}
