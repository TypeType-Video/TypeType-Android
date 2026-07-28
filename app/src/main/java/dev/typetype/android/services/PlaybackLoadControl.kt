package dev.typetype.android.services

import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl

@UnstableApi
internal fun createPlaybackLoadControl(): DefaultLoadControl =
    DefaultLoadControl.Builder()
        .setBufferDurationsMs(
            MIN_BUFFER_MS,
            MAX_BUFFER_MS,
            BUFFER_FOR_PLAYBACK_MS,
            BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS,
        )
        .setPrioritizeTimeOverSizeThresholds(true)
        .setBackBuffer(BACK_BUFFER_MS, true)
        .build()

private const val MIN_BUFFER_MS = 30_000
private const val MAX_BUFFER_MS = 30_000
private const val BUFFER_FOR_PLAYBACK_MS = 2_000
private const val BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS = 3_000
private const val BACK_BUFFER_MS = 30_000
