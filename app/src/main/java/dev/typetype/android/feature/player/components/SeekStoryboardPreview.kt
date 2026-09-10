package dev.typetype.android.feature.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.typetype.android.domain.stream.StreamStoryboardFrame

internal const val PLAYER_SEEK_STORYBOARD_PREVIEW_TAG = "player_seek_storyboard_preview"

private val PREVIEW_WIDTH = 160.dp
private val PREVIEW_HEIGHT = 90.dp

@Composable
internal fun SeekStoryboardPreview(
    frame: StreamStoryboardFrame?,
    timecode: String,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.testTag(PLAYER_SEEK_STORYBOARD_PREVIEW_TAG),
    ) {
        frame?.let { StoryboardFrame(frame = it) }
        Text(
            text = timecode,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .padding(top = 4.dp)
                .background(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                    shape = RoundedCornerShape(8.dp),
                )
                .padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun StoryboardFrame(frame: StreamStoryboardFrame) {
    val scale = PREVIEW_WIDTH.value / frame.width
    val spriteWidth = (frame.width * scale).dp
    val spriteHeight = (frame.height * scale).dp
    Box(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                shape = RoundedCornerShape(12.dp),
            )
            .padding(3.dp),
    ) {
        Box(
            modifier = Modifier
                .size(PREVIEW_WIDTH, PREVIEW_HEIGHT)
                .clipToBounds(),
        ) {
            AsyncImage(
                model = frame.url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .requiredSize(
                        width = spriteWidth,
                        height = spriteHeight,
                    )
                    .offset(x = (-frame.x * scale).dp, y = (-frame.y * scale).dp),
            )
        }
    }
}
