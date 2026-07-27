package dev.typetype.android.core.ui.components

import android.text.format.DateUtils
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
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
import dev.typetype.android.domain.feed.Video
import dev.typetype.android.domain.feed.VideoAvailability
import dev.typetype.android.domain.feed.availabilityAt
import dev.typetype.android.domain.feed.releaseTimeMillis

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoCard(
    video: Video,
    modifier: Modifier = Modifier,
    onMenuAction: ((VideoMenuAction) -> Unit)? = null,
    menuItemState: VideoMenuItemState = VideoMenuItemState(),
    onClick: () -> Unit = {},
    onChannelClick: (() -> Unit)? = null,
) {
    var menuVisible by remember { mutableStateOf(false) }
    var availabilityVisible by remember { mutableStateOf(false) }
    val availability = video.availabilityAt(System.currentTimeMillis())
    val metadata = video.metadataText()

    Column(
        modifier = modifier.fillMaxWidth().combinedClickable(
            onClick = {
                if (availability == VideoAvailability.Playable) onClick() else availabilityVisible = true
            },
            onLongClick = if (onMenuAction != null) ({ menuVisible = true }) else null,
            role = Role.Button,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
            )
            if (menuItemState.isWatched) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(Color.Black.copy(alpha = 0.45f)),
                )
                WatchedBadge(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp))
            }
            VideoThumbnailBadges(
                video = video,
                edgePadding = 8.dp,
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.Top) {
            val avatarModifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .let {
                    if (onChannelClick != null) {
                        it.combinedClickable(onClick = onChannelClick, role = Role.Button)
                    } else {
                        it
                    }
                }
            AsyncImage(
                model = video.uploaderAvatarUrl,
                contentDescription = if (onChannelClick != null) {
                    stringResource(R.string.video_open_channel_accessibility, video.uploaderName)
                } else {
                    null
                },
                contentScale = ContentScale.Crop,
                modifier = avatarModifier,
            )
            Spacer(Modifier.width(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = video.uploaderName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = if (onChannelClick != null) {
                        Modifier.combinedClickable(onClick = onChannelClick, role = Role.Button)
                    } else {
                        Modifier
                    },
                )
                metadata?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            }
            if (onMenuAction != null) {
                VideoMoreActionsButton(onClick = { menuVisible = true })
            }
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

@Composable
private fun WatchedBadge(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color.Black.copy(alpha = 0.7f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Visibility,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = stringResource(R.string.watched_indicator),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = Color.White,
        )
    }
}

@Composable
private fun Video.metadataText(): String? {
    val views = formatViews(viewCount)?.let { stringResource(R.string.video_views_short, it) }
    val published = releaseTimeMillis()?.let(::formatPublishedDate)
    return listOfNotNull(views, published).takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

private fun formatViews(views: Long): String? = when {
    views < 0L -> null
    views >= 1_000_000_000 -> "%.1fB".format(views / 1_000_000_000.0)
    views >= 1_000_000 -> "%.1fM".format(views / 1_000_000.0)
    views >= 1_000 -> "%.1fK".format(views / 1_000.0)
    else -> views.toString()
}

private fun formatPublishedDate(timestampMillis: Long): String =
    DateUtils.getRelativeTimeSpanString(
        timestampMillis,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE,
    ).toString()
