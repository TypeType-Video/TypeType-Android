package dev.typetype.android.feature.player

import androidx.media3.common.PlaybackException
import org.junit.Assert.assertEquals
import org.junit.Test

class RuntimeCodecFallbackTest {
    @Test
    fun `declared unsupported formats are rejected for good`() {
        assertEquals(
            CodecFailure.Lasting,
            codecFailure(PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED, emptyList()),
        )
        assertEquals(
            CodecFailure.Lasting,
            codecFailure(PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES, emptyList()),
        )
    }

    @Test
    fun `decoder initialization failures last only for this session`() {
        assertEquals(
            CodecFailure.ThisSession,
            codecFailure(
                PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
                listOf("Failed to initialize c2.android.av1.decoder"),
            ),
        )
        assertEquals(
            CodecFailure.ThisSession,
            codecFailure(PlaybackException.ERROR_CODE_DECODER_INIT_FAILED, listOf("")),
        )
    }

    @Test
    fun `a failed surface never rejects the codec`() {
        assertEquals(
            CodecFailure.None,
            codecFailure(
                PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
                listOf("setOutputSurface failed"),
            ),
        )
    }

    @Test
    fun `network and stream failures never poison codec support`() {
        assertEquals(
            CodecFailure.None,
            codecFailure(
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                listOf("connection reset"),
            ),
        )
        assertEquals(
            CodecFailure.None,
            codecFailure(PlaybackException.ERROR_CODE_DECODING_FAILED, listOf("malformed sample")),
        )
    }
}
