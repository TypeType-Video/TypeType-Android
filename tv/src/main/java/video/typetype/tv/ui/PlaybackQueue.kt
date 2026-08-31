package video.typetype.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.ui.text.style.TextOverflow
import coil3.compose.AsyncImage
import video.typetype.sdk.core.Video

@Composable
internal fun PlaybackQueue(
    videos: List<Video>,
    isAdvancing: Boolean,
    error: String?,
    onPlay: (Video) -> Unit,
) {
    val queueVideos = remember(videos) {
        videos.distinctBy { it.serviceId to it.id.value }.take(10)
    }
    val firstItemFocus = remember { FocusRequester() }
    LaunchedEffect(queueVideos.firstOrNull()?.id, isAdvancing) {
        if (queueVideos.isNotEmpty() && !isAdvancing) firstItemFocus.requestFocus()
    }
    Column(
        modifier = Modifier.fillMaxWidth().background(
            Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .94f), Color.Black)),
        ).padding(top = 52.dp, bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            if (isAdvancing) "Opening next video" else "Up next",
            modifier = Modifier.padding(horizontal = 38.dp),
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
        )
        error?.let {
            Text(it, modifier = Modifier.padding(horizontal = 38.dp), color = MaterialTheme.colorScheme.error)
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 38.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            itemsIndexed(queueVideos, key = { _, item -> "${item.serviceId}:${item.id.value}" }) { index, next ->
                QueueCard(next, index == 0, firstItemFocus, !isAdvancing) { onPlay(next) }
            }
        }
    }
}

@Composable
private fun QueueCard(
    video: Video,
    requestInitialFocus: Boolean,
    initialFocus: FocusRequester,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Surface(
        modifier = Modifier.width(286.dp)
            .then(if (requestInitialFocus) Modifier.focusRequester(initialFocus) else Modifier)
            .border(
                BorderStroke(if (focused) 3.dp else 0.dp, if (focused) Color.White else Color.Transparent),
                RoundedCornerShape(8.dp),
            ),
        enabled = enabled,
        onClick = onClick,
        interactionSource = interaction,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color(0xFF1A1C20),
            contentColor = Color.White,
            focusedContainerColor = Color(0xFF24272D),
            focusedContentColor = Color.White,
        ),
    ) {
        Column {
            AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = video.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(161.dp),
            )
            Text(
                video.title,
                modifier = Modifier.padding(11.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = Color.White,
            )
        }
    }
}

@Composable
internal fun PlayerLoading(error: String?) {
    Surface(shape = RoundedCornerShape(12.dp)) {
        Text(
            error ?: "Connecting to the TypeType playback session",
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 16.dp),
        )
    }
}

@Composable
internal fun NoServerMedia(message: String? = null, onClose: () -> Unit) {
    Surface(modifier = Modifier.padding(40.dp), onClick = onClose) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(message ?: "No server media is available for this video.", style = MaterialTheme.typography.titleLarge)
            Text(
                "Press Select to return.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
