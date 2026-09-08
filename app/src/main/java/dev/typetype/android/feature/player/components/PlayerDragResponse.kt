package dev.typetype.android.feature.player.components

import kotlin.math.abs
import kotlin.math.sign

internal fun swipeSeekTarget(startMs: Long, dragX: Float, durationMs: Long): Long {
    if (durationMs <= 0 || !dragX.isFinite()) return startMs
    val normalTravelMs = abs(dragX.toDouble()) * 100.0
    val travelMs = minOf(normalTravelMs, 60_000.0) +
        (normalTravelMs - 60_000.0).coerceAtLeast(0.0) * 10.0
    return (startMs.toDouble() + sign(dragX) * travelMs)
        .coerceIn(0.0, durationMs.toDouble()).toLong()
}

internal class HoldSpeedSteps(private val stepPx: Float) {
    var factor: Float = 2f
        private set
    private var anchorY = 0f

    fun update(dragY: Float): Float {
        val distance = dragY - anchorY
        val threshold = stepPx.coerceAtLeast(1f)
        if (abs(distance) >= threshold) {
            val steps = (abs(distance) / threshold).toInt()
            factor = (factor - sign(distance) * steps * 0.25f).coerceIn(0.25f, 4f)
            anchorY += sign(distance) * steps * threshold
        }
        return factor
    }
}
