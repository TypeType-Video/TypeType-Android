package dev.typetype.android.feature.player.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import dev.typetype.android.core.ui.components.TypeTypeSwitch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
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
    var visibleCount by remember(videos) { mutableIntStateOf(INITIAL_RELATED_VIDEO_COUNT) }
    val view = LocalView.current
    var loaderNearViewport by remember(videos) { mutableStateOf(false) }
    val videosToRender = visibleVideos.take(visibleCount)
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
            TypeTypeSwitch(checked = autoplayEnabled, onCheckedChange = onAutoplayChange)
        }
        videosToRender.forEach { video ->
            RelatedVideoCard(
                video = video,
                menuItemState = menuScope.stateFor(video),
                onMenuAction = { action -> menuScope.onAction(action, video) },
                onClick = { onPlayVideo(video.url) },
                onChannelClick = { onOpenChannel(video.uploaderUrl) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (visibleVideos.size > visibleCount) {
            LaunchedEffect(loaderNearViewport, visibleCount) {
                if (loaderNearViewport && visibleVideos.size > visibleCount) {
                    visibleCount = (visibleCount + RELATED_VIDEO_PAGE_SIZE)
                        .coerceAtMost(visibleVideos.size)
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .onGloballyPositioned { layoutCoordinates ->
                        loaderNearViewport = layoutCoordinates.positionInRoot().y <=
                            view.height + RELATED_AUTO_LOAD_MARGIN_PX
                    },
            )
        }
    }
}

private const val INITIAL_RELATED_VIDEO_COUNT = 6
private const val RELATED_VIDEO_PAGE_SIZE = 12
private const val RELATED_AUTO_LOAD_MARGIN_PX = 420f
