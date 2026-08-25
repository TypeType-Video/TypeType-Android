package dev.typetype.android.services

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackBufferPolicyTest {
    @Test
    fun `low-memory devices receive the smallest bounded budget`() {
        assertEquals(
            32 * MEBIBYTE,
            playbackBufferPolicy(memoryClassMb = 256, isLowRamDevice = true)
                .targetBufferBytes,
        )
        assertEquals(
            32 * MEBIBYTE,
            playbackBufferPolicy(memoryClassMb = 96, isLowRamDevice = false)
                .targetBufferBytes,
        )
    }

    @Test
    fun `medium heaps receive a moderate bounded budget`() {
        assertEquals(
            64 * MEBIBYTE,
            playbackBufferPolicy(memoryClassMb = 128, isLowRamDevice = false)
                .targetBufferBytes,
        )
    }

    @Test
    fun `large heaps remain capped below the Media3 video default`() {
        val policy = playbackBufferPolicy(
            memoryClassMb = 512,
            isLowRamDevice = false,
        )

        assertEquals(96 * MEBIBYTE, policy.targetBufferBytes)
        assertEquals(15_000, policy.minBufferMs)
        assertEquals(30_000, policy.maxBufferMs)
        assertEquals(15_000, policy.backBufferMs)
    }

    private companion object {
        const val MEBIBYTE = 1024 * 1024
    }
}
