package dev.typetype.android.core.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
fun HorizontalVideoCard(
    video: Video,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onChannelClick: (() -> Unit)? = null,
    onMenuAction: ((VideoMenuAction) -> Unit)? = null,
    menuItemState: VideoMenuItemState = VideoMenuItemState(),
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
    val openVideo = {
        if (availability == VideoAvailability.Playable) onClick() else availabilityVisible = true
    }
    val rootModifier = modifier
        .width(260.dp)
        .let { base ->
            if (onMenuAction != null) {
                base.combinedClickable(
                    onClick = openVideo,
                    onLongClick = { menuVisible = true },
                    role = Role.Button,
                )
            } else {
                base.clickable(onClick = openVideo, role = Role.Button)
            }
        }

    Column(modifier = rootModifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            AsyncImage(
                model = branding.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
            )
            VideoThumbnailBadges(
                video = video,
                edgePadding = 6.dp,
                compact = true,
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Top) {
            if (video.uploaderAvatarUrl.isNotBlank()) {
                val avatarModifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .let {
                        if (onChannelClick != null) {
                            it.clickable(onClick = onChannelClick, role = Role.Button)
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
                Spacer(Modifier.width(8.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = branding.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = video.uploaderName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = if (onChannelClick != null) {
                        Modifier.clickable(onClick = onChannelClick, role = Role.Button)
                    } else {
                        Modifier
                    },
                )
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
