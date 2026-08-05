package dev.typetype.android.core.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.typetype.android.R
import dev.typetype.android.core.ui.branding.rememberVideoBranding
import dev.typetype.android.domain.feed.Video
import dev.typetype.android.domain.feed.VideoAvailability
import dev.typetype.android.domain.feed.availabilityAt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RelatedVideoCard(
    video: Video,
    modifier: Modifier = Modifier,
    menuItemState: VideoMenuItemState = VideoMenuItemState(),
    onMenuAction: ((VideoMenuAction) -> Unit)? = null,
    onClick: () -> Unit = {},
    onChannelClick: () -> Unit = {},
) {
    var menuVisible by remember { mutableStateOf(false) }
    var availabilityVisible by remember { mutableStateOf(false) }
    val availability = video.availabilityAt(System.currentTimeMillis())
    val branding = rememberVideoBranding(
        sourceUrl = video.url,
        title = video.title,
        thumbnailUrl = video.thumbnailUrl,
        durationSeconds = video.durationSeconds,
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (availability == VideoAvailability.Playable) {
                        onClick()
                    } else {
                        availabilityVisible = true
                    }
                },
                onLongClick = if (onMenuAction == null) null else ({ menuVisible = true }),
                role = Role.Button,
            ),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .width(148.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            AsyncImage(
                model = branding.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
            )
            if (menuItemState.isWatched) {
                Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.35f)))
            }
            VideoThumbnailBadges(
                video = video,
                edgePadding = 5.dp,
                compact = true,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = branding.title,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = video.uploaderAvatarUrl,
                    contentDescription = stringResource(
                        R.string.video_open_channel_accessibility,
                        video.uploaderName,
                    ),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .combinedClickable(onClick = onChannelClick, role = Role.Button),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = video.uploaderName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.combinedClickable(
                        onClick = onChannelClick,
                        role = Role.Button,
                    ),
                )
            }
            Text(
                text = stringResource(R.string.video_views_short, formatRelatedViews(video.viewCount)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outlineVariant,
                maxLines = 1,
            )
        }
        if (onMenuAction != null) {
            VideoMoreActionsButton(onClick = { menuVisible = true })
        }
    }

    if (menuVisible && onMenuAction != null) {
        VideoCardMenu(
            onAction = onMenuAction,
            onDismiss = { menuVisible = false },
            state = menuItemState,
        )
    }
    if (availabilityVisible) {
        VideoAvailabilityDialog(
            availability = availability,
            onDismiss = { availabilityVisible = false },
        )
    }
}

private fun formatRelatedViews(views: Long): String = when {
    views >= 1_000_000_000 -> "%.1fB".format(views / 1_000_000_000.0)
    views >= 1_000_000 -> "%.1fM".format(views / 1_000_000.0)
    views >= 1_000 -> "%.1fK".format(views / 1_000.0)
    else -> views.toString()
}
