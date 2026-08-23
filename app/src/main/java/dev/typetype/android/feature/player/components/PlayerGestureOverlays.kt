package dev.typetype.android.feature.player.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import dev.typetype.android.feature.player.state.GestureSide
import dev.typetype.android.feature.player.state.PlayerGestureState
import kotlin.math.abs
import kotlin.math.sin

private const val SEEK_HINT_VISIBLE_MS = 650

@Composable
internal fun SeekHintOverlay(state: PlayerGestureState) {
    val side = state.seekHintSide.value
    val pulse = state.seekHintPulse.longValue
    val animation = remember { Animatable(0f) }
    LaunchedEffect(side, pulse) {
        if (side != null) {
            animation.snapTo(0f)
            animation.animateTo(
                targetValue = 1f,
                animationSpec = tween(SEEK_HINT_VISIBLE_MS, easing = LinearEasing),
            )
            state.seekHintSide.value = null
        }
    }
    if (side != null) {
        SeekTapWave(
            side = side,
            progress = animation.value,
            seconds = state.seekHintSeconds.floatValue.toInt(),
        )
    }
}

@Composable
internal fun PlaybackHintOverlay(state: PlayerGestureState) {
    val playing = state.playbackHintPlaying.value
    val pulse = state.playbackHintPulse.longValue
    LaunchedEffect(playing, pulse) {
        if (playing != null) {
            kotlinx.coroutines.delay(SEEK_HINT_VISIBLE_MS.toLong())
            state.playbackHintPlaying.value = null
        }
    }
    if (playing != null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.Black.copy(alpha = 0.68f))
                    .padding(18.dp),
            ) {
                Icon(
                    imageVector = if (playing) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                    contentDescription = null,
                    tint = Color.White,
                )
            }
        }
    }
}

@Composable
private fun SeekTapWave(
    side: GestureSide,
    progress: Float,
    seconds: Int,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.48f)
                .align(if (side == GestureSide.Left) Alignment.CenterStart else Alignment.CenterEnd),
        ) {
            val originX = if (side == GestureSide.Left) size.width * 0.7f else size.width * 0.3f
            val origin = Offset(originX, size.height / 2f)
            repeat(3) { index ->
                val phase = (progress - index * 0.18f).coerceIn(0f, 1f)
                if (phase <= 0f || phase >= 1f) return@repeat
                val radius = size.minDimension * (0.08f + phase * 0.5f)
                drawArc(
                    color = Color.White.copy(alpha = (1f - phase) * 0.9f),
                    startAngle = if (side == GestureSide.Left) 125f else -55f,
                    sweepAngle = 110f,
                    useCenter = false,
                    topLeft = Offset(origin.x - radius, origin.y - radius),
                    size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 3.dp.toPx(),
                        cap = StrokeCap.Round,
                    ),
                )
            }
        }
        Text(
            text = if (side == GestureSide.Left) "-${seconds}s" else "+${seconds}s",
            modifier = Modifier
                .align(if (side == GestureSide.Left) Alignment.CenterStart else Alignment.CenterEnd)
                .padding(horizontal = 56.dp),
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall,
        )
    }
}

@Composable
internal fun SeekDragOverlay(state: PlayerGestureState, durationMs: Long) {
    if (!state.seekDragOverlayActive.value) return
    val target = state.seekDragTargetMs.longValue
    val delta = target - state.seekDragStartMs.longValue
    val sign = if (delta >= 0) "+" else "-"
    val deltaSec = abs(delta) / 1000
    val targetText = formatTimeMs(target)
    val durationText = if (durationMs > 0) " / ${formatTimeMs(durationMs)}" else ""
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .width(288.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.Black.copy(alpha = 0.72f))
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "${sign}${deltaSec}s ($targetText$durationText)",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
            )
            SeekWave(
                fraction = if (durationMs > 0L) target / durationMs.toFloat() else 0f,
                modifier = Modifier.fillMaxWidth().height(32.dp),
            )
        }
    }
}

@Composable
internal fun SpeedBoostBadge(visible: Boolean, factor: Float) {
    if (!visible) return
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.65f))
                .padding(horizontal = 14.dp, vertical = 6.dp),
        ) {
            Text(
                text = "${factor}x",
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun SeekWave(
    fraction: Float,
    modifier: Modifier = Modifier,
) {
    val activeColor = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier) {
        val centerY = size.height / 2f
        val progressX = size.width * fraction.coerceIn(0f, 1f)
        val bars = 32
        val spacing = size.width / bars
        repeat(bars) { index ->
            val x = spacing * (index + 0.5f)
            val amplitude = 0.2f + abs(sin(index * 0.82f)) * 0.8f
            val halfHeight = size.height * amplitude * 0.42f
            drawLine(
                color = if (x <= progressX) activeColor else Color.White.copy(alpha = 0.3f),
                start = Offset(x, centerY - halfHeight),
                end = Offset(x, centerY + halfHeight),
                strokeWidth = if (x <= progressX) 3.dp.toPx() else 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
        drawCircle(
            color = activeColor,
            radius = 4.dp.toPx(),
            center = Offset(progressX, centerY),
        )
    }
}

private fun formatTimeMs(ms: Long): String {
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
