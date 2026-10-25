package dev.typetype.android.feature.player.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import dev.typetype.android.domain.stream.SponsorBlockSegment

private val TRACK_HEIGHT = 3.dp
private val COMPACT_TRACK_HEIGHT = 2.dp
private val THUMB_WIDTH = 14.dp
private val COMPACT_THUMB_WIDTH = 10.dp
private val SCRUBBING_TRACK_HEIGHT = 6.dp
private val SCRUBBING_THUMB_SIZE = 22.dp

@Composable
internal fun TimelineTrack(
    positionMs: Long,
    durationMs: Long,
    segments: List<SponsorBlockSegment>,
    compact: Boolean,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
    expanded: Boolean = false,
    interactive: Boolean = true,
    onScrub: (Long) -> Unit,
    onScrubFinished: (Long) -> Unit,
    onScrubCancelled: () -> Unit,
    accessibilityLabel: String,
    accessibilityStateDescription: String,
) {
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = if (expanded) Color.White.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.38f)
    val timelineModifier = if (interactive) {
        modifier
            .semantics {
                contentDescription = accessibilityLabel
                stateDescription = accessibilityStateDescription
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = positionMs.toFloat(),
                    range = 0f..durationMs.coerceAtLeast(1L).toFloat(),
                )
                setProgress { target ->
                    val targetMs = target.toLong().coerceIn(0L, durationMs.coerceAtLeast(0L))
                    onScrub(targetMs)
                    onScrubFinished(targetMs)
                    true
                }
            }
            .pointerInput(durationMs) {
                detectTapGestures { offset ->
                    val targetMs = offset.x.toPositionMs(size.width.toFloat(), durationMs)
                    onScrub(targetMs)
                    onScrubFinished(targetMs)
                }
            }
            .pointerInput(durationMs) {
                var lastTargetMs = positionMs
                detectDragGestures(
                    onDragStart = { offset ->
                        lastTargetMs = offset.x.toPositionMs(size.width.toFloat(), durationMs)
                        onScrub(lastTargetMs)
                    },
                    onDrag = { change, _ ->
                        lastTargetMs = change.position.x.toPositionMs(size.width.toFloat(), durationMs)
                        onScrub(lastTargetMs)
                    },
                    onDragEnd = { onScrubFinished(lastTargetMs) },
                    onDragCancel = onScrubCancelled,
                )
            }
    } else {
        modifier
    }
    Box(
        modifier = timelineModifier,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val targetTrackHeight = when {
                expanded -> if (emphasized) 10.dp else 6.dp
                emphasized -> SCRUBBING_TRACK_HEIGHT
                compact -> COMPACT_TRACK_HEIGHT
                else -> TRACK_HEIGHT
            }
            val targetThumbSize = when {
                expanded -> if (emphasized) 32.dp else 24.dp
                emphasized -> SCRUBBING_THUMB_SIZE
                compact -> COMPACT_THUMB_WIDTH
                else -> THUMB_WIDTH
            }
            val trackHeight = targetTrackHeight.toPx()
            val thumbWidth = targetThumbSize.toPx()
            val thumbHeight = targetThumbSize.toPx()
            val trackTop = (size.height - trackHeight) / 2f
            val trackRadius = trackHeight / 2f
            val progress = if (durationMs > 0) positionMs.toFloat() / durationMs.toFloat() else 0f
            val progressX = progress.coerceIn(0f, 1f) * size.width
            drawRoundRect(
                color = inactiveColor,
                topLeft = Offset(0f, trackTop),
                size = Size(size.width, trackHeight),
                cornerRadius = CornerRadius(trackRadius, trackRadius),
            )
            drawRoundRect(
                color = activeColor,
                topLeft = Offset(0f, trackTop),
                size = Size(progressX, trackHeight),
                cornerRadius = CornerRadius(trackRadius, trackRadius),
            )
            if (durationMs > 0) segments.forEach { segment ->
                val startFraction = (segment.startMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                val endFraction = (segment.endMs.toFloat() / durationMs.toFloat()).coerceIn(startFraction, 1f)
                val xStart = startFraction * size.width
                val xEnd = endFraction * size.width
                drawRoundRect(
                    color = sponsorBlockColorForCategory(segment.category),
                    topLeft = Offset(xStart, trackTop),
                    size = Size((xEnd - xStart).coerceAtLeast(3f), trackHeight),
                    cornerRadius = CornerRadius(trackRadius, trackRadius),
                )
            }
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(
                    x = playerTimeBarThumbStartX(
                        progressX = progressX,
                        trackWidth = size.width,
                        thumbWidth = thumbWidth,
                    ),
                    y = (size.height - thumbHeight) / 2f,
                ),
                size = Size(thumbWidth, thumbHeight),
                cornerRadius = CornerRadius(thumbWidth / 2f, thumbWidth / 2f),
            )
        }
    }
}

internal fun playerTimeBarThumbStartX(
    progressX: Float,
    trackWidth: Float,
    thumbWidth: Float,
): Float {
    val maximumStartX = (trackWidth - thumbWidth).coerceAtLeast(0f)
    return (progressX - thumbWidth / 2f).coerceIn(0f, maximumStartX)
}

private fun Float.toPositionMs(width: Float, durationMs: Long): Long {
    if (durationMs <= 0 || width <= 0f) return 0L
    return ((this / width).coerceIn(0f, 1f) * durationMs).toLong()
}
