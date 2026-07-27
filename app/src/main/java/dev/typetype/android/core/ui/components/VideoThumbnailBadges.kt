package dev.typetype.android.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.typetype.android.domain.feed.Video
import dev.typetype.android.domain.feed.VideoAvailability
import dev.typetype.android.domain.feed.availabilityAt

@Composable
internal fun BoxScope.VideoThumbnailBadges(
    video: Video,
    edgePadding: Dp = 8.dp,
    compact: Boolean = false,
) {
    val availability = video.availabilityAt(System.currentTimeMillis())
    if (availability == VideoAvailability.MembersOnly) {
        VideoMembershipBadge(
            compact = compact,
            modifier = Modifier.align(Alignment.TopStart).padding(edgePadding),
        )
    }
    VideoStatusBadge(
        video = video,
        compact = compact,
        modifier = Modifier.align(Alignment.BottomStart).padding(edgePadding),
    )
    if (!video.isLive && availability != VideoAvailability.Scheduled && video.durationSeconds > 0L) {
        VideoDurationBadge(
            durationSeconds = video.durationSeconds,
            modifier = Modifier.align(Alignment.BottomEnd).padding(edgePadding),
        )
    }
}

@Composable
private fun VideoDurationBadge(durationSeconds: Long, modifier: Modifier = Modifier) {
    Text(
        text = formatVideoDuration(durationSeconds),
        style = MaterialTheme.typography.labelSmall,
        color = Color.White,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color.Black.copy(alpha = 0.82f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

private fun formatVideoDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val remaining = seconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, remaining)
    } else {
        "%d:%02d".format(minutes, remaining)
    }
}
