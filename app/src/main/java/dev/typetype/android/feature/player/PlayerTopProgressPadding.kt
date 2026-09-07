package dev.typetype.android.feature.player

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import kotlin.math.roundToInt

internal fun Modifier.playerTopProgressPadding(
    maxTopPx: Float,
    progress: () -> Float,
): Modifier = layout { measurable, constraints ->
    val topPx = (maxTopPx * (1f - progress().coerceIn(0f, 1f))).roundToInt()
    val maxHeight = if (constraints.hasBoundedHeight) {
        (constraints.maxHeight - topPx).coerceAtLeast(0)
    } else {
        constraints.maxHeight
    }
    val childConstraints = constraints.copy(
        minHeight = constraints.minHeight.coerceAtMost(maxHeight),
        maxHeight = maxHeight,
    )
    val placeable = measurable.measure(childConstraints)
    layout(placeable.width, placeable.height + topPx) {
        placeable.placeRelative(0, topPx)
    }
}
