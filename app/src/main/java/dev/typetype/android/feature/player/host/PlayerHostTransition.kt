package dev.typetype.android.feature.player.host

import kotlin.math.roundToInt

internal data class PlayerHostTransition(
    val progress: Float,
    val heightPx: Int,
    val offsetPx: Int,
    val miniContentAlpha: Float,
    val isSettledMini: Boolean,
)

internal fun playerHostTransition(
    offsetPx: Float,
    miniAnchorPx: Float,
    containerHeightPx: Float,
    miniHeightPx: Float,
    isAnimationRunning: Boolean,
): PlayerHostTransition {
    val safeOffset = offsetPx.takeIf(Float::isFinite) ?: 0f
    val progress = if (miniAnchorPx > 0f) {
        (safeOffset / miniAnchorPx).coerceIn(0f, 1f)
    } else {
        0f
    }
    val height = containerHeightPx + (miniHeightPx - containerHeightPx) * progress
    return PlayerHostTransition(
        progress = progress,
        heightPx = height.roundToInt().coerceAtLeast(1),
        offsetPx = safeOffset.roundToInt(),
        miniContentAlpha = ((progress - 0.55f) / 0.45f).coerceIn(0f, 1f),
        isSettledMini = progress >= 0.999f && !isAnimationRunning,
    )
}
