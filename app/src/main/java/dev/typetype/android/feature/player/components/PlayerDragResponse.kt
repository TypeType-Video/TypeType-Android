package dev.typetype.android.feature.player.components

import kotlin.math.abs
import kotlin.math.sign

internal fun proportionalSeekTarget(startMs: Long, dragX: Float, width: Float, durationMs: Long): Long {
    if (durationMs <= 0 || width <= 0 || !dragX.isFinite()) return startMs
    val fraction = (dragX / width).coerceIn(-1f, 1f)
    val rangeMs = minOf(durationMs, 180_000L)
    val accelerated = fraction * (0.35f + 0.65f * abs(fraction))
    return (startMs + (accelerated * rangeMs).toLong()).coerceIn(0L, durationMs)
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
