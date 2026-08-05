package dev.typetype.android.feature.player.components

import androidx.annotation.OptIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.state.rememberProgressStateWithTickInterval
import dev.typetype.android.domain.stream.SponsorBlockSegment
import kotlin.time.Duration.Companion.milliseconds

private const val TICK_INTERVAL_MS = 200L
private val TIME_LABEL_WIDTH = 52.dp
private val COMPACT_TIME_LABEL_MIN_WIDTH = 30.dp
private val TIMELINE_HEIGHT = 36.dp
private val COMPACT_TIMELINE_HEIGHT = 18.dp
private val TRACK_HEIGHT = 7.dp
private val COMPACT_TRACK_HEIGHT = 5.dp
private val THUMB_WIDTH = 14.dp
private val THUMB_HEIGHT = 22.dp
private val COMPACT_THUMB_WIDTH = 12.dp
private val COMPACT_THUMB_HEIGHT = 18.dp

@OptIn(markerClass = [UnstableApi::class])
@Composable
fun PlayerTimeBar(
    player: Player,
    modifier: Modifier = Modifier,
    segments: List<SponsorBlockSegment> = emptyList(),
    compact: Boolean = false,
) {
    val progressState = rememberProgressStateWithTickInterval(player, TICK_INTERVAL_MS)
    var scrubPositionMs by remember { mutableStateOf<Long?>(null) }

    val durationMs = progressState.durationMs.coerceAtLeast(0L)
    val displayedPosMs = scrubPositionMs ?: progressState.currentPositionMs.coerceIn(0L, durationMs)

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = formatTime(displayedPosMs),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
            color = Color.White,
            modifier = if (compact) {
                Modifier.widthIn(min = COMPACT_TIME_LABEL_MIN_WIDTH)
            } else {
                Modifier.width(TIME_LABEL_WIDTH)
            },
            textAlign = TextAlign.End,
        )
        TimelineTrack(
            positionMs = displayedPosMs,
            durationMs = durationMs,
            segments = segments,
            compact = compact,
            onScrub = { scrubPositionMs = it },
            onScrubFinished = { targetMs ->
                player.seekTo(targetMs)
                scrubPositionMs = null
            },
            onScrubCancelled = { scrubPositionMs = null },
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = if (compact) 2.dp else 4.dp)
                .height(if (compact) COMPACT_TIMELINE_HEIGHT else TIMELINE_HEIGHT),
        )
        Text(
            text = formatTime(durationMs),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
            color = Color.White.copy(alpha = 0.7f),
            modifier = if (compact) {
                Modifier.widthIn(min = COMPACT_TIME_LABEL_MIN_WIDTH)
            } else {
                Modifier.width(TIME_LABEL_WIDTH)
            },
        )
    }
}

@Composable
internal fun TimelineTrack(
    positionMs: Long,
    durationMs: Long,
    segments: List<SponsorBlockSegment>,
    compact: Boolean,
    onScrub: (Long) -> Unit,
    onScrubFinished: (Long) -> Unit,
    onScrubCancelled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = Color.White.copy(alpha = 0.3f)
    Box(
        modifier = modifier
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
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val trackHeight = if (compact) COMPACT_TRACK_HEIGHT.toPx() else TRACK_HEIGHT.toPx()
            val thumbWidth = if (compact) COMPACT_THUMB_WIDTH.toPx() else THUMB_WIDTH.toPx()
            val thumbHeight = if (compact) COMPACT_THUMB_HEIGHT.toPx() else THUMB_HEIGHT.toPx()
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
            segments.forEach { segment ->
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
                color = activeColor,
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

private fun formatTime(ms: Long): String {
    val total = ms.milliseconds.inWholeSeconds
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
