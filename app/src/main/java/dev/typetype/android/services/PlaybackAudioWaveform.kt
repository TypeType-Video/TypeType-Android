package dev.typetype.android.services

import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.TeeAudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.sqrt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@UnstableApi
@Singleton
class PlaybackAudioWaveform @Inject constructor() : TeeAudioProcessor.AudioBufferSink {
    private val mutableLevels = MutableStateFlow(FloatArray(BAR_COUNT))
    val levels: StateFlow<FloatArray> = mutableLevels.asStateFlow()

    private var encoding = C.ENCODING_INVALID
    private var lastEmissionNanos = 0L

    override fun flush(sampleRateHz: Int, channelCount: Int, encoding: Int) {
        this.encoding = encoding
        lastEmissionNanos = 0L
        mutableLevels.value = FloatArray(BAR_COUNT)
    }

    override fun handleBuffer(buffer: ByteBuffer) {
        val now = System.nanoTime()
        if (now - lastEmissionNanos < EMISSION_INTERVAL_NANOS) return
        val measured = measurePcmWaveform(buffer, encoding, BAR_COUNT) ?: return
        val previous = mutableLevels.value
        mutableLevels.value = FloatArray(BAR_COUNT) { index ->
            previous[index] * SMOOTHING_RETAINED + measured[index] * SMOOTHING_NEW
        }
        lastEmissionNanos = now
    }
}

internal fun measurePcmWaveform(
    source: ByteBuffer,
    encoding: Int,
    barCount: Int,
): FloatArray? {
    val bytesPerSample = when (encoding) {
        C.ENCODING_PCM_8BIT -> 1
        C.ENCODING_PCM_16BIT -> 2
        C.ENCODING_PCM_24BIT -> 3
        C.ENCODING_PCM_32BIT, C.ENCODING_PCM_FLOAT -> 4
        else -> return null
    }
    val buffer = source.duplicate().order(ByteOrder.LITTLE_ENDIAN)
    val sampleCount = buffer.remaining() / bytesPerSample
    if (sampleCount == 0 || barCount <= 0) return FloatArray(max(0, barCount))
    val basePosition = buffer.position()
    return FloatArray(barCount) { barIndex ->
        val firstSample = barIndex * sampleCount / barCount
        val lastSample = max(firstSample + 1, (barIndex + 1) * sampleCount / barCount)
            .coerceAtMost(sampleCount)
        val stride = max(1, (lastSample - firstSample) / MAX_SAMPLES_PER_BAR)
        var sumSquares = 0.0
        var measuredSamples = 0
        var sampleIndex = firstSample
        while (sampleIndex < lastSample) {
            val offset = basePosition + sampleIndex * bytesPerSample
            val sample = readPcmSample(buffer, offset, encoding)
            sumSquares += sample * sample
            measuredSamples += 1
            sampleIndex += stride
        }
        val rms = sqrt(sumSquares / max(1, measuredSamples)).toFloat()
        (rms * LEVEL_GAIN).coerceIn(0f, 1f)
    }
}

private fun readPcmSample(buffer: ByteBuffer, offset: Int, encoding: Int): Float =
    when (encoding) {
        C.ENCODING_PCM_8BIT -> ((buffer.get(offset).toInt() and 0xff) - 128) / 128f
        C.ENCODING_PCM_16BIT -> buffer.getShort(offset) / 32768f
        C.ENCODING_PCM_24BIT -> {
            val raw = (buffer.get(offset).toInt() and 0xff) or
                ((buffer.get(offset + 1).toInt() and 0xff) shl 8) or
                (buffer.get(offset + 2).toInt() shl 16)
            raw / 8388608f
        }
        C.ENCODING_PCM_32BIT -> buffer.getInt(offset) / 2147483648f
        C.ENCODING_PCM_FLOAT -> buffer.getFloat(offset).coerceIn(-1f, 1f)
        else -> 0f
    }

private const val BAR_COUNT = 24
private const val MAX_SAMPLES_PER_BAR = 32
private const val LEVEL_GAIN = 2.2f
private const val SMOOTHING_RETAINED = 0.45f
private const val SMOOTHING_NEW = 0.55f
private const val EMISSION_INTERVAL_NANOS = 50_000_000L
