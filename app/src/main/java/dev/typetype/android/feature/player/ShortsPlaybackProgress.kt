package dev.typetype.android.feature.player

import androidx.annotation.OptIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.state.rememberProgressStateWithTickInterval
import dev.typetype.android.R
import dev.typetype.android.feature.player.components.TimelineTrack
import dev.typetype.android.feature.player.components.formatPlayerTime

@OptIn(markerClass = [UnstableApi::class])
@Composable
internal fun ShortsPlaybackProgress(
    player: Player,
    fallbackDurationMs: Long,
    modifier: Modifier = Modifier,
) {
    val progress = rememberProgressStateWithTickInterval(player, SHORTS_PROGRESS_TICK_MS)
    val durationMs = progress.durationMs.takeIf { it > 0L } ?: fallbackDurationMs.coerceAtLeast(0L)
    var scrubPositionMs by remember { mutableStateOf<Long?>(null) }
    val positionMs = scrubPositionMs ?: progress.currentPositionMs
    TimelineTrack(
        positionMs = positionMs,
        durationMs = durationMs,
        segments = emptyList(),
        compact = false,
        onScrub = { scrubPositionMs = it },
        onScrubFinished = {
            if (durationMs > 0L) player.seekTo(it)
            scrubPositionMs = null
        },
        onScrubCancelled = { scrubPositionMs = null },
        accessibilityLabel = stringResource(R.string.player_timeline),
        accessibilityStateDescription = stringResource(
            R.string.player_timeline_position,
            formatPlayerTime(positionMs),
            formatPlayerTime(durationMs),
        ),
        modifier = modifier.fillMaxWidth().height(36.dp),
    )
}

private const val SHORTS_PROGRESS_TICK_MS = 100L
