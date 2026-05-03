package dev.typetype.android.feature.player

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.session.MediaController
import coil3.compose.AsyncImage
import dev.typetype.android.R
import dev.typetype.android.domain.stream.Stream
import dev.typetype.android.feature.player.components.PlayerSurfaceBox
import dev.typetype.android.feature.player.components.rememberMediaController

@Composable
fun PlayerRoute(
    onNavigateBack: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PlayerScreen(state = state, onNavigateBack = onNavigateBack)
}

@Composable
fun PlayerScreen(state: PlayerState, onNavigateBack: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        when {
            state.isLoading -> LoadingState()
            state.errorMessage != null -> ErrorState(
                message = state.errorMessage,
                onNavigateBack = onNavigateBack,
            )
            state.stream != null -> LoadedPlayer(
                stream = state.stream,
                onNavigateBack = onNavigateBack,
            )
        }
    }
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun ErrorState(message: String, onNavigateBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier.align(Alignment.Start),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.player_back),
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
        Spacer(Modifier.height(48.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun LoadedPlayer(stream: Stream, onNavigateBack: () -> Unit) {
    val controllerState = rememberMediaController()
    val controller = controllerState.value
    val scrollState = rememberScrollState()

    LaunchedEffect(stream.id, controller) {
        controller?.let { ctrl -> bindStreamToController(ctrl, stream) }
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            if (controller != null) {
                PlayerSurfaceBox(
                    player = controller,
                    onNavigateBack = onNavigateBack,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
        StreamMetadata(stream = stream)
    }
}

private fun bindStreamToController(controller: MediaController, stream: Stream) {
    val (sourceUrl, mimeType) = pickPlayableSource(stream)
    if (sourceUrl == null) return
    val metadata = MediaMetadata.Builder()
        .setTitle(stream.title)
        .setArtist(stream.uploaderName)
        .setArtworkUri(Uri.parse(stream.thumbnailUrl))
        .build()
    val mediaItem = MediaItem.Builder()
        .setUri(sourceUrl)
        .setMediaId(stream.id)
        .setMediaMetadata(metadata)
        .apply { mimeType?.let { setMimeType(it) } }
        .build()
    val sameMedia = controller.currentMediaItem?.mediaId == stream.id
    if (!sameMedia) {
        controller.setMediaItem(mediaItem)
        controller.prepare()
        if (stream.startPositionMillis > 0) controller.seekTo(stream.startPositionMillis)
    }
    controller.playWhenReady = true
}

@Composable
private fun StreamMetadata(stream: Stream) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text(
            text = stream.title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.2).sp,
            ),
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = stream.uploaderAvatarUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
            Spacer(Modifier.size(10.dp))
            Column {
                Text(
                    text = stream.uploaderName,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "${formatViews(stream.viewCount)} views · " +
                        "${formatViews(stream.likeCount)} likes",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (stream.description.isNotBlank()) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = stream.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun pickPlayableSource(stream: Stream): Pair<String?, String?> = when {
    !stream.hlsUrl.isNullOrBlank() -> stream.hlsUrl to MimeTypes.APPLICATION_M3U8
    !stream.dashMpdUrl.isNullOrBlank() -> stream.dashMpdUrl to MimeTypes.APPLICATION_MPD
    !stream.progressiveUrl.isNullOrBlank() -> stream.progressiveUrl to MimeTypes.VIDEO_MP4
    else -> null to null
}

private fun formatViews(value: Long): String = when {
    value >= 1_000_000_000 -> "%.1fB".format(value / 1_000_000_000.0)
    value >= 1_000_000 -> "%.1fM".format(value / 1_000_000.0)
    value >= 1_000 -> "%.1fK".format(value / 1_000.0)
    else -> value.toString()
}
