package dev.typetype.android.feature.player.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.state.rememberProgressStateWithTickInterval
import kotlin.time.Duration.Companion.milliseconds

private const val TICK_INTERVAL_MS = 200L

@OptIn(markerClass = [UnstableApi::class])
@Composable
fun PlayerTimeBar(player: Player, modifier: Modifier = Modifier) {
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
        )
        Slider(
            value = if (durationMs > 0) displayedPosMs.toFloat() / durationMs.toFloat() else 0f,
            onValueChange = { fraction ->
                if (durationMs > 0) scrubPositionMs = (fraction * durationMs).toLong()
            },
            onValueChangeFinished = {
                scrubPositionMs?.let { player.seekTo(it) }
                scrubPositionMs = null
            },
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = Color.White.copy(alpha = 0.3f),
            ),
        )
        Text(
            text = formatTime(durationMs),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
            color = Color.White.copy(alpha = 0.7f),
        )
    }
}

private fun formatTime(ms: Long): String {
    val total = ms.milliseconds.inWholeSeconds
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
