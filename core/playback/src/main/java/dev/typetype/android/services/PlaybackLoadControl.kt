package dev.typetype.android.services

import android.app.ActivityManager
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl

@UnstableApi
internal fun createPlaybackLoadControl(activityManager: ActivityManager): DefaultLoadControl =
    createPlaybackLoadControl(
        playbackBufferPolicy(
            memoryClassMb = activityManager.memoryClass,
            isLowRamDevice = activityManager.isLowRamDevice,
        ),
    )

@UnstableApi
internal fun createPlaybackLoadControl(policy: PlaybackBufferPolicy): DefaultLoadControl =
    DefaultLoadControl.Builder()
        .setBufferDurationsMs(
            policy.minBufferMs,
            policy.maxBufferMs,
            policy.bufferForPlaybackMs,
            policy.bufferForPlaybackAfterRebufferMs,
        )
        .setTargetBufferBytes(policy.targetBufferBytes)
        .setPrioritizeTimeOverSizeThresholds(false)
        .setBackBuffer(policy.backBufferMs, true)
        .build()

internal fun playbackBufferPolicy(
    memoryClassMb: Int,
    isLowRamDevice: Boolean,
): PlaybackBufferPolicy {
    val targetBufferBytes = when {
        isLowRamDevice || memoryClassMb < MEMORY_CLASS_128_MB -> LOW_RAM_TARGET_BUFFER_BYTES
        memoryClassMb < MEMORY_CLASS_192_MB -> MEDIUM_HEAP_TARGET_BUFFER_BYTES
        else -> LARGE_HEAP_TARGET_BUFFER_BYTES
    }
    return PlaybackBufferPolicy(targetBufferBytes = targetBufferBytes)
}

internal data class PlaybackBufferPolicy(
    val minBufferMs: Int = 15_000,
    val maxBufferMs: Int = 30_000,
    val bufferForPlaybackMs: Int = 2_000,
    val bufferForPlaybackAfterRebufferMs: Int = 3_000,
    val backBufferMs: Int = 15_000,
    val targetBufferBytes: Int,
)

private const val MEMORY_CLASS_128_MB = 128
private const val MEMORY_CLASS_192_MB = 192
private const val MEBIBYTE = 1024 * 1024
private const val LOW_RAM_TARGET_BUFFER_BYTES = 32 * MEBIBYTE
private const val MEDIUM_HEAP_TARGET_BUFFER_BYTES = 64 * MEBIBYTE
private const val LARGE_HEAP_TARGET_BUFFER_BYTES = 96 * MEBIBYTE
