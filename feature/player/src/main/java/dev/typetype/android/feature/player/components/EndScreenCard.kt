package dev.typetype.android.feature.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.typetype.android.R
import dev.typetype.android.domain.stream.EndScreenElement
import dev.typetype.android.domain.stream.EndScreenElementType

@Composable
internal fun EndScreenCard(
    element: EndScreenElement,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val round = element.type == EndScreenElementType.Channel ||
        element.type == EndScreenElementType.Website
    val shape = if (round) CircleShape else RoundedCornerShape(8.dp)
    BoxWithConstraints(
        modifier = modifier
            .clip(shape)
            .background(Color.Black.copy(alpha = 0.4f))
            .border(1.dp, Color.White.copy(alpha = 0.25f), shape)
            .clickable(
                onClickLabel = stringResource(element.type.actionLabel()),
                role = Role.Button,
                onClick = onClick,
            )
            .semantics { contentDescription = element.title },
    ) {
        if (element.thumbnailUrl.isNotBlank()) {
            AsyncImage(
                model = element.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (!round && maxWidth >= TITLE_MIN_CARD_WIDTH) {
            Text(
                text = element.title,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))))
                    .padding(start = 6.dp, end = 6.dp, top = 12.dp, bottom = 4.dp),
            )
        }
    }
}

private fun EndScreenElementType.actionLabel(): Int = when (this) {
    EndScreenElementType.Video -> R.string.end_screen_play_video
    EndScreenElementType.Playlist -> R.string.end_screen_open_playlist
    EndScreenElementType.Channel -> R.string.end_screen_open_channel
    EndScreenElementType.Website -> R.string.end_screen_open_link
}

private val TITLE_MIN_CARD_WIDTH = 96.dp
