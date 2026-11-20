package dev.typetype.android.feature.live

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.typetype.android.R
import dev.typetype.android.core.ui.components.ProfileAvatar
import dev.typetype.android.core.ui.components.VideoCard
import dev.typetype.android.domain.feed.Video
import dev.typetype.android.domain.twitch.TwitchChannel

// A row that scrolls sideways, so the followed sections stay short and the popular lives stay in sight below them.
@Composable
internal fun LiveVideoRail(videos: List<Video>, onPlayVideo: (String) -> Unit, onOpenChannel: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(end = 16.dp)) {
        items(videos, key = { it.url }, contentType = { "rail-video" }) { video ->
            VideoCard(
                video = video,
                onClick = { onPlayVideo(video.url) },
                onChannelClick = { onOpenChannel(video.uploaderUrl) },
                modifier = Modifier.width(RAIL_CARD_WIDTH),
            )
        }
    }
}

@Composable
internal fun LiveChannelRail(channels: List<TwitchChannel>, onOpenChannel: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(end = 16.dp)) {
        items(channels, key = { it.login }, contentType = { "rail-channel" }) { channel ->
            val live = channel.live != null
            val description = if (live) {
                stringResource(R.string.live_channel_live_description, channel.displayName)
            } else {
                channel.displayName
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(CHANNEL_WIDTH)
                    .clickable(role = Role.Button) { onOpenChannel(channel.url) }
                    .semantics(mergeDescendants = true) { contentDescription = description },
            ) {
                ProfileAvatar(
                    imageUrl = channel.avatarUrl,
                    fallbackLetter = channel.displayName.take(1),
                    contentDescription = null,
                    modifier = Modifier
                        .size(AVATAR_SIZE)
                        .then(if (live) Modifier.border(2.dp, LIVE_RED, CircleShape).padding(3.dp) else Modifier),
                )
                Text(
                    text = channel.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

private val RAIL_CARD_WIDTH = 260.dp
private val CHANNEL_WIDTH = 72.dp
private val AVATAR_SIZE = 64.dp
private val LIVE_RED = Color(0xFFE91916)
