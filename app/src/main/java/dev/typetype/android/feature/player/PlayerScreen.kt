package dev.typetype.android.feature.player

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.session.MediaController
import dev.typetype.android.R
import dev.typetype.android.domain.stream.Stream
import dev.typetype.android.feature.player.components.DescriptionSection
import dev.typetype.android.feature.player.components.PlayerSurfaceBox
import dev.typetype.android.feature.player.components.RelatedStreamsSection
import dev.typetype.android.feature.player.components.UploaderCard
import dev.typetype.android.feature.player.components.rememberMediaController

@Composable
fun PlayerRoute(
    onNavigateBack: () -> Unit,
    onPlayVideo: (videoUrl: String) -> Unit,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PlayerScreen(
        state = state,
        onNavigateBack = onNavigateBack,
        onPlayVideo = onPlayVideo,
    )
}

@Composable
fun PlayerScreen(
    state: PlayerState,
    onNavigateBack: () -> Unit,
    onPlayVideo: (videoUrl: String) -> Unit,
) {
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
                onPlayVideo = onPlayVideo,
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
private fun LoadedPlayer(
    stream: Stream,
    onNavigateBack: () -> Unit,
    onPlayVideo: (videoUrl: String) -> Unit,
) {
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
                    sponsorBlockSegments = stream.sponsorBlockSegments,
                    chapters = stream.chapters,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DescriptionSection(
                title = stream.title,
                viewCount = stream.viewCount,
                likeCount = stream.likeCount,
                description = stream.description,
            )
            UploaderCard(
                name = stream.uploaderName,
                avatarUrl = stream.uploaderAvatarUrl,
                subscriberCount = stream.uploaderSubscriberCount,
                verified = stream.uploaderVerified,
            )
            Spacer(Modifier.height(4.dp))
            RelatedStreamsSection(
                videos = stream.relatedStreams,
                onPlayVideo = onPlayVideo,
            )
        }
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

private fun pickPlayableSource(stream: Stream): Pair<String?, String?> = when {
    !stream.hlsUrl.isNullOrBlank() -> stream.hlsUrl to MimeTypes.APPLICATION_M3U8
    !stream.dashMpdUrl.isNullOrBlank() -> stream.dashMpdUrl to MimeTypes.APPLICATION_MPD
    !stream.progressiveUrl.isNullOrBlank() -> stream.progressiveUrl to MimeTypes.VIDEO_MP4
    else -> null to null
}
