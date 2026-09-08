package dev.typetype.android.feature.player.components

internal class TimelineSeekGesture(
    startMs: Long,
    private val durationMs: Long,
    private val viewportWidthPx: Float,
    private val density: Float,
) {
    private var position = startMs.toDouble().coerceIn(0.0, durationMs.coerceAtLeast(0).toDouble())
    var fineSeeking = false
        private set

    fun move(deltaX: Float, downwardTravelPx: Float): Long {
        if (durationMs <= 0 || viewportWidthPx <= 0 || density <= 0 ||
            !viewportWidthPx.isFinite() || !density.isFinite() ||
            !deltaX.isFinite() || !downwardTravelPx.isFinite()
        ) return position.toLong()

        val downwardDp = downwardTravelPx / density
        fineSeeking = when {
            downwardDp >= 48f -> true
            downwardDp <= 32f -> false
            else -> fineSeeking
        }
        val normalMsPerPixel = durationMs.toDouble() / viewportWidthPx
        val msPerPixel = if (fineSeeking) {
            minOf(normalMsPerPixel / 10.0, 100.0 / density)
        } else {
            normalMsPerPixel
        }
        position = (position + deltaX * msPerPixel).coerceIn(0.0, durationMs.toDouble())
        return position.toLong()
    }
}
