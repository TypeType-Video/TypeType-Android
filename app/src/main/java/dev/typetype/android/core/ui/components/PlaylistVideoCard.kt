package dev.typetype.android.core.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
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
import dev.typetype.android.domain.library.PlaylistVideo
import dev.typetype.android.domain.library.VideoMeta

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlaylistVideoCard(
    video: PlaylistVideo,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    isWatched: Boolean,
    modifier: Modifier = Modifier,
    meta: VideoMeta? = null,
    onChannelClick: ((channelUrl: String) -> Unit)? = null,
    onMoreClick: (() -> Unit)? = null,
) {
    val avatarUrl = video.channelAvatarUrl.takeIf { it.isNotBlank() }
        ?: meta?.channelAvatarUrl?.takeIf { it.isNotBlank() }
    val channelName = video.channelName.takeIf { it.isNotBlank() }
        ?: meta?.channelName?.takeIf { it.isNotBlank() }
    val channelUrl = video.channelUrl.takeIf { it.isNotBlank() }
        ?: meta?.channelUrl?.takeIf { it.isNotBlank() }
    val hasChannelInfo = avatarUrl != null || channelName != null
    val channelActionDescription = when {
        onChannelClick == null || channelUrl == null -> null
        channelName != null -> stringResource(R.string.video_open_channel_accessibility, channelName)
        else -> stringResource(R.string.video_menu_open_channel)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongPress, role = Role.Button)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            if (isWatched) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f)),
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
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
            if (onMoreClick != null) {
                VideoMoreActionsButton(
                    onClick = onMoreClick,
                    overlay = true,
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                )
            }
            if (video.durationSeconds > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.85f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = formatDuration(video.durationSeconds),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        if (hasChannelInfo) {
            Row(verticalAlignment = Alignment.Top) {
                val avatarModifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .let { base ->
                        if (onChannelClick != null && channelUrl != null) {
                            base.combinedClickable(
                                onClick = { onChannelClick(channelUrl) },
                                role = Role.Button,
                            )
                        } else {
                            base
                        }
                    }
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = channelActionDescription,
                    contentScale = ContentScale.Crop,
                    modifier = avatarModifier,
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (channelName != null) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = channelName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = if (onChannelClick != null && channelUrl != null) {
                                Modifier.combinedClickable(
                                    onClick = { onChannelClick(channelUrl) },
                                    role = Role.Button,
                                )
                            } else {
                                Modifier
                            },
                        )
                    }
                }
            }
        } else {
            Text(
                text = video.title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
