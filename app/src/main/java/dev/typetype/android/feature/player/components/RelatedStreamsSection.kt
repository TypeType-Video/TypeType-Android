package dev.typetype.android.feature.player.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.typetype.android.R
import dev.typetype.android.core.ui.components.RelatedVideoCard
import dev.typetype.android.domain.feed.Video
import dev.typetype.android.feature.menu.VideoMenuScope

@Composable
fun RelatedStreamsSection(
    videos: List<Video>,
    onPlayVideo: (videoUrl: String) -> Unit,
    menuScope: VideoMenuScope,
    onOpenChannel: (channelUrl: String) -> Unit,
    autoplayEnabled: Boolean,
    onAutoplayChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val visibleVideos = videos.filterNot { menuScope.isHidden(it) }
    if (visibleVideos.isEmpty()) return
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.player_up_next),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.player_autoplay_short),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 8.dp),
            )
            Switch(checked = autoplayEnabled, onCheckedChange = onAutoplayChange)
        }
        visibleVideos.forEach { video ->
            RelatedVideoCard(
                video = video,
                menuItemState = menuScope.stateFor(video),
                onMenuAction = { action -> menuScope.onAction(action, video) },
                onClick = { onPlayVideo(video.url) },
                onChannelClick = { onOpenChannel(video.uploaderUrl) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
