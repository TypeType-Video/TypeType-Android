package video.typetype.tv.ui

import androidx.compose.foundation.gestures.BringIntoViewSpec

internal object TvBringIntoViewSpec : BringIntoViewSpec {
    override fun calculateScrollDistance(
        offset: Float,
        size: Float,
        containerSize: Float,
    ): Float {
        val targetLeadingEdge = containerSize * CONTENT_PIVOT_FRACTION
        return offset - targetLeadingEdge
    }
}

private const val CONTENT_PIVOT_FRACTION = .16f
