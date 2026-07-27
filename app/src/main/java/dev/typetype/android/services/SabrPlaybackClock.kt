package dev.typetype.android.services

import java.util.concurrent.atomic.AtomicLong

internal class SabrPlaybackClock {
    private val positionUs = AtomicLong()

    fun update(positionMs: Long) {
        positionUs.set(Math.multiplyExact(positionMs.coerceAtLeast(0L), 1_000L))
    }

    fun currentPositionUs(): Long = positionUs.get()
}
