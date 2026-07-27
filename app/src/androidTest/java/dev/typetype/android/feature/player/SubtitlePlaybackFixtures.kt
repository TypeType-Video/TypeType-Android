package dev.typetype.android.feature.player

import java.nio.ByteBuffer
import java.nio.ByteOrder

internal fun silentWaveFixture(): ByteArray {
    val sampleRate = 8_000
    val dataSize = sampleRate * 2 * 20
    return ByteBuffer.allocate(44 + dataSize)
        .order(ByteOrder.LITTLE_ENDIAN)
        .put("RIFF".toByteArray())
        .putInt(36 + dataSize)
        .put("WAVE".toByteArray())
        .put("fmt ".toByteArray())
        .putInt(16)
        .putShort(1)
        .putShort(1)
        .putInt(sampleRate)
        .putInt(sampleRate * 2)
        .putShort(2)
        .putShort(16)
        .put("data".toByteArray())
        .putInt(dataSize)
        .array()
}
