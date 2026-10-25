package dev.typetype.android.feature.player.components

import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.state.rememberProgressStateWithTickInterval
import dev.typetype.android.R
import dev.typetype.android.domain.stream.SponsorBlockSegment
import dev.typetype.android.domain.stream.StreamStoryboard
import kotlin.time.Duration.Companion.milliseconds

private const val TICK_INTERVAL_MS = 200L
private val TIME_LABEL_WIDTH = 52.dp
private val COMPACT_TIME_LABEL_MIN_WIDTH = 30.dp
private val TIMELINE_HEIGHT = 36.dp
private val COMPACT_TIMELINE_HEIGHT = 18.dp
private val CLASSIC_PREVIEW_WIDTH = 160.dp
private val CLASSIC_PREVIEW_LIFT = 52.dp


@OptIn(markerClass = [UnstableApi::class])
@Composable
fun PlayerTimeBar(
    player: Player,
    modifier: Modifier = Modifier,
    storyboard: StreamStoryboard? = null,
    segments: List<SponsorBlockSegment> = emptyList(),
    compact: Boolean = false,
    expanded: Boolean = false,
    previewPositionMs: Long? = null,
    onScrubbingChange: (Boolean) -> Unit = {},
) {
    val progressState = rememberProgressStateWithTickInterval(player, TICK_INTERVAL_MS)
    var scrubPositionMs by remember { mutableStateOf<Long?>(null) }

    val durationMs = progressState.durationMs.coerceAtLeast(0L)
    val displayedPosMs = scrubPositionMs
        ?: previewPositionMs?.takeIf { durationMs > 0L }
        ?: progressState.currentPositionMs.coerceIn(0L, durationMs)
    val emphasized = scrubPositionMs != null || previewPositionMs != null
    val positionLabel = formatPlayerTime(displayedPosMs)
    val durationLabel = formatPlayerTime(durationMs)

    BoxWithConstraints(modifier = modifier) {
        val scrubFraction = if (durationMs > 0L) {
            (displayedPosMs.toFloat() / durationMs).coerceIn(0f, 1f)
        } else {
            0f
        }
        val previewOffsetX = (maxWidth - CLASSIC_PREVIEW_WIDTH).coerceAtLeast(0.dp) * scrubFraction

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = positionLabel,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = if (expanded) 16.sp else 12.sp),
                color = Color.White,
                modifier = if (compact) {
                    Modifier.widthIn(min = COMPACT_TIME_LABEL_MIN_WIDTH)
                } else {
                    Modifier.width(if (expanded) 68.dp else TIME_LABEL_WIDTH)
                },
                textAlign = TextAlign.End,
            )
            TimelineTrack(
                positionMs = displayedPosMs,
                durationMs = durationMs,
                segments = segments,
                compact = compact,
                expanded = expanded,
                emphasized = emphasized,
                onScrub = {
                    onScrubbingChange(true)
                    scrubPositionMs = it
                },
                onScrubFinished = { targetMs ->
                    player.seekTo(targetMs)
                    onScrubbingChange(false)
                    scrubPositionMs = null
                },
                onScrubCancelled = {
                    onScrubbingChange(false)
                    scrubPositionMs = null
                },
                accessibilityLabel = stringResource(R.string.player_timeline),
                accessibilityStateDescription = stringResource(
                    R.string.player_timeline_position,
                    positionLabel,
                    durationLabel,
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = if (compact) 2.dp else 4.dp)
                    .height(if (expanded) 56.dp else if (compact) COMPACT_TIMELINE_HEIGHT else TIMELINE_HEIGHT),
            )
            Text(
                text = durationLabel,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = if (expanded) 16.sp else 12.sp),
                color = Color.White.copy(alpha = 0.7f),
                modifier = if (compact) {
                    Modifier.widthIn(min = COMPACT_TIME_LABEL_MIN_WIDTH)
                } else {
                    Modifier.width(if (expanded) 68.dp else TIME_LABEL_WIDTH)
                },
            )
        }

        val classicScrubPositionMs = scrubPositionMs
        if (classicScrubPositionMs != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = previewOffsetX, y = -CLASSIC_PREVIEW_LIFT),
            ) {
                SeekStoryboardPreview(
                    frame = storyboard?.frameAt(classicScrubPositionMs),
                    timecode = formatPlayerTime(classicScrubPositionMs),
                )
            }
        }
    }
}

@Composable
internal fun PlayerSeekScrubOverlay(
    player: Player,
    positionMs: Long,
    segments: List<SponsorBlockSegment>,
    isFullscreen: Boolean,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        val expanded = LocalConfiguration.current.smallestScreenWidthDp >= 600 && maxWidth >= 600.dp
        TimelineTrack(
            positionMs = positionMs,
            durationMs = player.duration.coerceAtLeast(0L),
            segments = segments,
            compact = false,
            emphasized = true,
            expanded = expanded,
            interactive = false,
            onScrub = {},
            onScrubFinished = {},
            onScrubCancelled = {},
            accessibilityLabel = "",
            accessibilityStateDescription = "",
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(if (expanded) 72.dp else if (isFullscreen) 52.dp else 40.dp)
                .padding(horizontal = 4.dp),
        )
    }
}

internal fun formatPlayerTime(ms: Long): String {
    val total = ms.milliseconds.inWholeSeconds
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
