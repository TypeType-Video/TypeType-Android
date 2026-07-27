package dev.typetype.android.services

import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

internal class SabrPlaybackClock {
    private val positionUs = AtomicLong()
    private val playbackRate = AtomicReference(1.0f)

    fun update(positionMs: Long, rate: Float) {
        positionUs.set(Math.multiplyExact(positionMs.coerceAtLeast(0L), 1_000L))
        if (rate.isFinite() && rate in 0.25f..4.0f) playbackRate.set(rate)
    }

    fun currentPositionUs(): Long = positionUs.get()

    fun currentPlaybackRate(): Float = playbackRate.get()
}
