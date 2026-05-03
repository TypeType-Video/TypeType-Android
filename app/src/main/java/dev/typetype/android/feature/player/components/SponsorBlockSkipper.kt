package dev.typetype.android.feature.player.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.media3.common.Player
import dev.typetype.android.domain.stream.SponsorAction
import dev.typetype.android.domain.stream.SponsorBlockSegment
import kotlinx.coroutines.delay

private const val POLL_INTERVAL_MS = 250L
private const val SKIP_GUARD_MS = 200L

@Composable
fun SponsorBlockSkipper(
    player: Player,
    segments: List<SponsorBlockSegment>,
    onSegmentSkipped: (SponsorBlockSegment) -> Unit = {},
) {
    val skippable = remember(segments) {
        segments.filter { it.action == SponsorAction.Skip && it.endMs > it.startMs }
    }
    if (skippable.isEmpty()) return

    LaunchedEffect(player, skippable) {
        while (true) {
            val pos = player.currentPosition
            val match = skippable.firstOrNull { segment ->
                pos in segment.startMs..(segment.endMs - SKIP_GUARD_MS)
            }
            if (match != null) {
                player.seekTo(match.endMs)
                onSegmentSkipped(match)
            }
            delay(POLL_INTERVAL_MS)
        }
    }
}
