package dev.typetype.android.services

import androidx.media3.common.C
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackAudioWaveformTest {
    @Test
    fun silenceProducesEmptyWaveform() {
        val buffer = ByteBuffer.allocate(96).order(ByteOrder.LITTLE_ENDIAN)
        repeat(48) { buffer.putShort(0) }
        buffer.flip()

        val levels = requireNotNull(measurePcmWaveform(buffer, C.ENCODING_PCM_16BIT, 6))

        assertTrue(levels.all { it == 0f })
    }

    @Test
    fun louderPcmRegionProducesTallerBarsWithoutConsumingInput() {
        val buffer = ByteBuffer.allocate(128).order(ByteOrder.LITTLE_ENDIAN)
        repeat(32) { buffer.putShort(1_000) }
        repeat(32) { buffer.putShort(20_000) }
        buffer.flip()
        val initialPosition = buffer.position()

        val levels = requireNotNull(measurePcmWaveform(buffer, C.ENCODING_PCM_16BIT, 2))

        assertEquals(initialPosition, buffer.position())
        assertTrue(levels[1] > levels[0] * 5f)
        assertTrue(levels.all { it in 0f..1f })
    }

    @Test
    fun unsupportedEncodingIsIgnored() {
        val levels = measurePcmWaveform(ByteBuffer.allocate(8), C.ENCODING_INVALID, 4)

        assertEquals(null, levels)
    }
}
