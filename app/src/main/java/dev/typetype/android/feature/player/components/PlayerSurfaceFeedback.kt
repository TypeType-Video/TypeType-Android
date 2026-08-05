package dev.typetype.android.feature.player.components

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import dev.typetype.android.feature.player.SponsorBlockPlaybackPolicy

@Composable
internal fun BoxScope.PlayerSurfaceFeedback(
    player: Player,
    isPlaying: Boolean,
    isInPip: Boolean,
    sponsorBlockPolicy: SponsorBlockPlaybackPolicy,
) {
    PipPlaybackStateOverlay(
        visible = isInPip,
        isPlaying = isPlaying,
        modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
    )
    SponsorBlockPlaybackFeedback(
        player = player,
        policy = sponsorBlockPolicy,
        visible = !isInPip,
        modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
    )
}
