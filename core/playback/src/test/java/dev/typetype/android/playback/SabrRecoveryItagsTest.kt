package dev.typetype.android.playback

import dev.typetype.android.domain.preferences.VP9_CODEC_KEY
import dev.typetype.android.playback.DecoderSupport
import dev.typetype.android.playback.SourceSelectionFixtures.FakeCodecSupport
import dev.typetype.android.playback.SourceSelectionFixtures.video
import dev.typetype.android.playback.playableLowerVideoItags
import org.junit.Assert.assertEquals
import org.junit.Test

class SabrRecoveryItagsTest {
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
    fun `recommended SABR recovery keeps the active codec`() {
        val selected = video("selected", height = 1080, codec = "avc1.640028", itag = 137)
        val lowerH264 = video("lower-h264", height = 720, codec = "avc1.64001f", itag = 136)
        val lowerVp9 = video("lower-vp9", height = 720, codec = "vp09.00.31.08", itag = 247)
        val support = FakeCodecSupport(
            video = mapOf(
                selected.url to DecoderSupport.Hardware,
                lowerH264.url to DecoderSupport.Hardware,
                lowerVp9.url to DecoderSupport.Hardware,
            ),
        )

        assertEquals(
            setOf(136),
            listOf(lowerVp9, selected, lowerH264).playableLowerVideoItags(
                selected,
                support,
            ),
        )
    }
}
