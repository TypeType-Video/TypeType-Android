package dev.typetype.android.feature.player.components

internal fun isWithinFullscreenGestureDeadzone(
    yPx: Float,
    heightPx: Float,
    deadzonePx: Float,
): Boolean {
    if (heightPx <= 0f || deadzonePx <= 0f) return false
    val boundedDeadzone = deadzonePx.coerceAtMost(heightPx / 2f)
    return yPx <= boundedDeadzone || yPx >= heightPx - boundedDeadzone
}
