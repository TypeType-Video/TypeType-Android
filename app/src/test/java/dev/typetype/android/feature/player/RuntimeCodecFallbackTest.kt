package dev.typetype.android.feature.player

import androidx.media3.common.PlaybackException
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RuntimeCodecFallbackTest {
    @Test
    fun `declared unsupported formats trigger smart fallback`() {
        assertTrue(
            isCodecCapabilityFailure(
                PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
                emptyList(),
            ),
        )
        assertTrue(
            isCodecCapabilityFailure(
                PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES,
                emptyList(),
            ),
        )
    }

    @Test
    fun `decoder initialization falls back unless the surface failed`() {
        assertTrue(
            isCodecCapabilityFailure(
                PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
                listOf("Failed to initialize c2.android.av1.decoder"),
            ),
        )
        assertFalse(
            isCodecCapabilityFailure(
                PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
                listOf("setOutputSurface failed"),
            ),
        )
    }

    @Test
    fun `network and stream failures never poison codec support`() {
        assertFalse(
            isCodecCapabilityFailure(
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                listOf("connection reset"),
            ),
        )
        assertFalse(
            isCodecCapabilityFailure(
                PlaybackException.ERROR_CODE_DECODING_FAILED,
                listOf("malformed sample"),
            ),
        )
    }
}
