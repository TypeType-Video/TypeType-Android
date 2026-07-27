package dev.typetype.android.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.typetype.android.R
import dev.typetype.android.domain.feed.Video
import dev.typetype.android.domain.feed.VideoBadgeStatus
import dev.typetype.android.domain.feed.badgeStatusAt

@Composable
internal fun VideoStatusBadge(
    video: Video,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val status = video.badgeStatusAt(System.currentTimeMillis()) ?: return
    Text(
        text = when (status) {
            VideoBadgeStatus.Live -> stringResource(R.string.video_live_badge)
            VideoBadgeStatus.Replay -> stringResource(R.string.video_replay_badge)
            VideoBadgeStatus.Premiere -> stringResource(R.string.video_premiere_badge)
            VideoBadgeStatus.Upcoming -> stringResource(R.string.video_upcoming_badge)
        },
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
        ),
        color = Color.White,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(status.backgroundColor())
            .padding(horizontal = if (compact) 5.dp else 7.dp, vertical = 2.dp),
    )
}

@Composable
private fun VideoBadgeStatus.backgroundColor(): Color = when (this) {
    VideoBadgeStatus.Live -> MaterialTheme.colorScheme.error
    VideoBadgeStatus.Premiere -> MaterialTheme.colorScheme.primary
    VideoBadgeStatus.Upcoming -> MaterialTheme.colorScheme.tertiary
    VideoBadgeStatus.Replay -> Color.Black.copy(alpha = 0.82f)
}
