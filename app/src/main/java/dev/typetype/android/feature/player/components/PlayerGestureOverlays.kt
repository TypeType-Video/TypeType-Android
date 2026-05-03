package dev.typetype.android.feature.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.typetype.android.feature.player.state.GestureSide
import dev.typetype.android.feature.player.state.PlayerGestureState
import kotlinx.coroutines.delay
import kotlin.math.abs

private const val SEEK_HINT_VISIBLE_MS = 600L

@Composable
internal fun SeekHintOverlay(state: PlayerGestureState) {
    val side = state.seekHintSide.value
    LaunchedEffect(side, state.seekHintSeconds.floatValue) {
        if (side != null) {
            delay(SEEK_HINT_VISIBLE_MS)
            state.seekHintSide.value = null
        }
    }
    if (side != null) {
        Box(modifier = Modifier.fillMaxSize()) {
            val alignment = if (side == GestureSide.Left) Alignment.CenterStart else Alignment.CenterEnd
            val sign = if (side == GestureSide.Left) "-" else "+"
            Box(
                modifier = Modifier
                    .align(alignment)
                    .padding(horizontal = 48.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Text(
                    text = "$sign${state.seekHintSeconds.floatValue.toInt()}s",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
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
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black.copy(alpha = 0.65f))
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Text(
                text = "${sign}${deltaSec}s ($targetText$durationText)",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
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
internal fun DragSliderOverlay(
    visible: Boolean,
    fraction: Float,
    label: String,
    modifier: Modifier = Modifier,
) {
    if (!visible) return
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .height(180.dp)
            .width(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "${(fraction * 100).toInt()}%",
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
        )
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .fillMaxHeight(0.7f)
                .width(4.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = Color.White.copy(alpha = 0.3f),
        )
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.8f),
            style = MaterialTheme.typography.labelSmall,
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
