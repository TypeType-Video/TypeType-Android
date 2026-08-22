package dev.typetype.android.feature.player

import androidx.annotation.OptIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.state.rememberProgressStateWithTickInterval

@OptIn(markerClass = [UnstableApi::class])
@Composable
internal fun ShortsPlaybackProgress(
    player: Player,
    modifier: Modifier = Modifier,
) {
    val progress = rememberProgressStateWithTickInterval(player, SHORTS_PROGRESS_TICK_MS)
    if (progress.durationMs <= 0L) return
    val fraction = (
        progress.currentPositionMs.toFloat() / progress.durationMs.toFloat()
    ).coerceIn(0f, 1f)
    LinearProgressIndicator(
        progress = { fraction },
        modifier = modifier.fillMaxWidth().height(2.dp),
        color = Color.White,
        trackColor = Color.White.copy(alpha = 0.24f),
        drawStopIndicator = {},
    )
}

private const val SHORTS_PROGRESS_TICK_MS = 100L
