package dev.typetype.android.playback

import dev.typetype.android.playback.SourceSelectionFixtures.FakeCodecSupport
import dev.typetype.android.playback.SourceSelectionFixtures.audio
import dev.typetype.android.playback.SourceSelectionFixtures.providerStream
import dev.typetype.android.playback.SourceSelectionFixtures.video
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Base64

class ProviderHlsSourceTest {
    private val low = video("https://watch.example/api/media/low", height = 360, codec = "")
        .copy(playbackMimeType = "application/vnd.apple.mpegurl", bitrate = null)
    private val high = video("https://watch.example/api/media/high", height = 720, codec = "")
        .copy(playbackMimeType = "application/vnd.apple.mpegurl", bitrate = null)
    private val sound = audio("https://watch.example/api/media/audio", locale = "ja").copy(bitrate = 192)
    private val support = FakeCodecSupport(
        video = mapOf(low.url to DecoderSupport.Hardware, high.url to DecoderSupport.Hardware),
        audio = mapOf(sound.url to DecoderSupport.Hardware),
    )

    private fun PlayableSource.master() = String(Base64.getDecoder().decode(url.substringAfter("base64,")))

    @Test
    fun `automatic quality lists every playlist with the audio group`() {
        val source = providerStream(listOf(low, high), listOf(sound)).pickHlsProviderSource(
            selectedQuality = "1080p",
            selectedAudioKey = null,
            defaultAudioLanguage = "en",
            preferOriginalLanguage = false,
            codecSupport = support,
        )

        assertEquals(
            """
            #EXTM3U
            #EXT-X-VERSION:6
            #EXT-X-INDEPENDENT-SEGMENTS
            #EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="audio",NAME="medium",DEFAULT=YES,AUTOSELECT=YES,URI="https://watch.example/api/media/audio"
            #EXT-X-STREAM-INF:BANDWIDTH=2692000,RESOLUTION=1280x720,CODECS="avc1.4d4020,mp4a.40.2",AUDIO="audio"
            https://watch.example/api/media/high
            #EXT-X-STREAM-INF:BANDWIDTH=992000,RESOLUTION=640x360,CODECS="avc1.4d4020,mp4a.40.2",AUDIO="audio"
            https://watch.example/api/media/low
            """.trimIndent() + "\n",
            source?.master(),
        )
    }

    @Test
    fun `explicit quality keeps only the chosen playlist`() {
        val source = providerStream(listOf(low, high), listOf(sound)).pickHlsProviderSource(
            selectedQuality = low.videoSelectionKey(),
            selectedAudioKey = null,
            defaultAudioLanguage = "en",
            preferOriginalLanguage = false,
            codecSupport = support,
        )

        assertEquals(listOf("https://watch.example/api/media/low"), source?.master()?.lines()?.filter { it.startsWith("https") })
        assertEquals(low.videoSelectionKey(), source?.sourceKey)
    }

    @Test
    fun `progressive provider video is left to the merged source`() {
        val progressive = video("https://watch.example/api/media/m4s", height = 720, codec = "avc1.640028")

        assertNull(
            providerStream(listOf(progressive), listOf(sound)).pickHlsProviderSource(
                selectedQuality = "720p",
                selectedAudioKey = null,
                defaultAudioLanguage = "en",
                preferOriginalLanguage = false,
                codecSupport = support,
            ),
        )
    }
}
