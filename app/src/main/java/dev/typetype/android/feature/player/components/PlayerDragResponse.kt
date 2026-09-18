package dev.typetype.android.feature.player.components

import kotlin.math.abs
import kotlin.math.sign

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
