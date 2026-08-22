package dev.typetype.android.core.ui.components

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
