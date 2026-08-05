package dev.typetype.android.feature.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import dev.typetype.android.R
import dev.typetype.android.domain.stream.SponsorBlockSegment
import dev.typetype.android.feature.player.SponsorBlockPlaybackPolicy
import kotlinx.coroutines.delay

@Composable
internal fun SponsorBlockCurrentSegment(
    player: Player,
    policy: SponsorBlockPlaybackPolicy,
    visible: Boolean,
) {
    var activeSegment by remember(policy.visibleSegments) {
        mutableStateOf<SponsorBlockSegment?>(null)
    }
    LaunchedEffect(player, policy.visibleSegments, policy.showCurrentSegment, visible) {
        if (!policy.showCurrentSegment || !visible) {
            activeSegment = null
            return@LaunchedEffect
        }
        while (true) {
            val positionMs = player.currentPosition
            activeSegment = policy.visibleSegments.firstOrNull {
                positionMs >= it.startMs && positionMs < it.endMs
            }
            delay(CURRENT_SEGMENT_POLL_INTERVAL_MS)
        }
    }

    AnimatedVisibility(
        visible = visible && policy.showCurrentSegment && activeSegment != null,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        val segment = activeSegment ?: return@AnimatedVisibility
        SponsorBlockCurrentSegmentCard(
            segment = segment,
            canSkip = policy.canManuallySkip(segment),
            onSkip = {
                player.seekTo(sponsorBlockSkipTargetMs(segment, player.duration))
            },
        )
    }
}

@Composable
private fun SponsorBlockCurrentSegmentCard(
    segment: SponsorBlockSegment,
    canSkip: Boolean,
    onSkip: () -> Unit,
) {
    Surface(
        color = Color.Black.copy(alpha = 0.82f),
        contentColor = Color.White,
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 6.dp, top = 7.dp, bottom = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(sponsorBlockColorForCategory(segment.category), CircleShape),
            )
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = stringResource(segment.category.labelResource()),
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    text = stringResource(R.string.player_sponsorblock_label),
                    color = Color.White.copy(alpha = 0.68f),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            if (canSkip) {
                TextButton(onClick = onSkip) {
                    Text(stringResource(R.string.player_sponsorblock_skip))
                }
            }
        }
    }
}

private const val CURRENT_SEGMENT_POLL_INTERVAL_MS = 250L
