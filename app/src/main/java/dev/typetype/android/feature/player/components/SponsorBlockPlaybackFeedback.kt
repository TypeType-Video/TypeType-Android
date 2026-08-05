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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import dev.typetype.android.domain.stream.SponsorCategory
import dev.typetype.android.feature.player.SponsorBlockPlaybackPolicy
import kotlinx.coroutines.delay

private const val SKIP_NOTICE_DURATION_MS = 2_600L

@Composable
internal fun SponsorBlockPlaybackFeedback(
    player: Player,
    policy: SponsorBlockPlaybackPolicy,
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    var skippedSegment by remember { mutableStateOf<SponsorBlockSegment?>(null) }
    var automaticAction by remember { mutableStateOf(SponsorBlockAutomaticAction.Skipped) }
    var skippedEvent by remember { mutableIntStateOf(0) }

    SponsorBlockSkipper(
        player = player,
        segments = policy.automaticSegments,
        muteInsteadOfSkip = policy.muteInsteadOfSkip,
    ) { segment, action ->
        skippedSegment = segment
        automaticAction = action
        skippedEvent += 1
    }
    LaunchedEffect(skippedEvent) {
        if (skippedEvent == 0) return@LaunchedEffect
        delay(SKIP_NOTICE_DURATION_MS)
        skippedSegment = null
    }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SponsorBlockCurrentSegment(player = player, policy = policy, visible = visible)
        SponsorBlockSkipNotice(
            segment = skippedSegment,
            action = automaticAction,
            visible = visible,
        )
    }
}

@Composable
internal fun SponsorBlockSkipNotice(
    segment: SponsorBlockSegment?,
    modifier: Modifier = Modifier,
    visible: Boolean = true,
    action: SponsorBlockAutomaticAction = SponsorBlockAutomaticAction.Skipped,
) {
    AnimatedVisibility(
        visible = visible && segment != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        val category = segment?.category ?: return@AnimatedVisibility
        Surface(
            color = Color.Black.copy(alpha = 0.82f),
            contentColor = Color.White,
            shape = RoundedCornerShape(12.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(sponsorBlockColorForCategory(category), CircleShape),
                )
                Column {
                    Text(
                        text = stringResource(
                            when (action) {
                                SponsorBlockAutomaticAction.Skipped -> {
                                    R.string.player_sponsorblock_skipped
                                }
                                SponsorBlockAutomaticAction.Muted -> {
                                    R.string.player_sponsorblock_muted
                                }
                            },
                        ),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        text = stringResource(category.labelResource()),
                        color = Color.White.copy(alpha = 0.68f),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}

internal fun SponsorCategory.labelResource(): Int = when (this) {
    SponsorCategory.Sponsor -> R.string.player_sponsorblock_category_sponsor
    SponsorCategory.SelfPromo -> R.string.player_sponsorblock_category_self_promotion
    SponsorCategory.ExclusiveAccess -> R.string.player_sponsorblock_category_exclusive_access
    SponsorCategory.Interaction -> R.string.player_sponsorblock_category_interaction
    SponsorCategory.Poi -> R.string.player_sponsorblock_category_highlight
    SponsorCategory.Intro -> R.string.player_sponsorblock_category_intro
    SponsorCategory.Outro -> R.string.player_sponsorblock_category_outro
    SponsorCategory.Preview -> R.string.player_sponsorblock_category_preview
    SponsorCategory.MusicOffTopic -> R.string.player_sponsorblock_category_music
    SponsorCategory.Filler -> R.string.player_sponsorblock_category_filler
    SponsorCategory.Chapter -> R.string.player_sponsorblock_category_chapter
    SponsorCategory.Unknown -> R.string.player_sponsorblock_category_other
}
