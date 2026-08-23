package dev.typetype.android.feature.player

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import androidx.media3.common.Format
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.typetype.android.domain.stream.StreamAudioSource
import dev.typetype.android.domain.stream.StreamVideoSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DevicePlaybackCodecSupportAndroidTest {
    private val support = DevicePlaybackCodecSupport(
        ApplicationProvider.getApplicationContext(),
    )

    @Test
    fun baselineH264AndAacRemainPlayable() {
        assertFalse(support.video(h264()) == DecoderSupport.Unsupported)
        assertFalse(support.audio(aac()) == DecoderSupport.Unsupported)
    }

    @Test
    fun unknownFrameRateDoesNotRejectBaselineH264() {
        assertFalse(support.video(h264().copy(fps = 0)) == DecoderSupport.Unsupported)
    }

    @Test
    fun runtimeDecoderFailureRejectsTheMatchingAdvertisedFormat() {
        val activeFormat = Format.Builder()
            .setCodecs("avc1.42001e")
            .setWidth(640)
            .setHeight(360)
            .build()

        assertTrue(support.rejectVideo(activeFormat))
        assertEquals(DecoderSupport.Unsupported, support.video(h264()))
    }

    @Test
    fun baselineH264DecoderCanStart() {
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, 640, 360)
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 30)

        startDecoder(MediaFormat.MIMETYPE_VIDEO_AVC, format)
    }

    @Test
    fun baselineAacDecoderCanStart() {
        val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, 44_100, 2)
        format.setInteger(
            MediaFormat.KEY_AAC_PROFILE,
            MediaCodecInfo.CodecProfileLevel.AACObjectLC,
        )

        startDecoder(MediaFormat.MIMETYPE_AUDIO_AAC, format)
    }

    private fun startDecoder(mimeType: String, format: MediaFormat) {
        val decoder = MediaCodec.createDecoderByType(mimeType)
        try {
            decoder.configure(format, null, null, 0)
            decoder.start()
            decoder.stop()
        } finally {
            decoder.release()
        }
    }

    private fun h264() = StreamVideoSource(
        url = "https://instance.example/video",
        mimeType = "video/mp4",
        codec = "avc1.42001e",
        resolution = "360p",
        width = 640,
        height = 360,
        fps = 30,
        bitrate = 800_000,
        isVideoOnly = true,
        itag = 134,
    )

    private fun aac() = StreamAudioSource(
        url = "https://instance.example/audio",
        mimeType = "audio/mp4",
        codec = "mp4a.40.2",
        bitrate = 128_000,
        quality = "medium",
        audioTrackId = null,
        audioTrackName = null,
        audioLocale = null,
        isOriginal = true,
        itag = 140,
    )
}
