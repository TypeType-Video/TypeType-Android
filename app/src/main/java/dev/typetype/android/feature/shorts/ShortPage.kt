package dev.typetype.android.feature.shorts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.typetype.android.R
import dev.typetype.android.core.ui.branding.rememberVideoBranding
import dev.typetype.android.core.ui.components.VideoMenuAction
import dev.typetype.android.core.ui.components.VideoMenuItemState
import dev.typetype.android.domain.feed.Video
import dev.typetype.android.feature.player.ShortsPlaybackProgress
import dev.typetype.android.feature.player.components.LocalMediaController
import dev.typetype.android.feature.player.components.rememberCurrentMediaId

internal data class ShortsPageVisuals(
    val overlayAlpha: Float,
    val overlayTranslationY: Float,
)

internal fun shortsPageVisuals(pageOffset: Float): ShortsPageVisuals {
    val distance = pageOffset.coerceIn(0f, 1f)
    return ShortsPageVisuals(
        overlayAlpha = 1f - distance * 0.62f,
        overlayTranslationY = distance * 24f,
    )
}

@Composable
internal fun ShortPage(
    video: Video,
    isActive: Boolean,
    embeddedPlaybackEnabled: Boolean,
    visuals: ShortsPageVisuals,
    onPlayVideo: (String) -> Unit,
    onOpenChannel: (String) -> Unit,
    menuItemState: VideoMenuItemState,
    onMenuAction: (VideoMenuAction) -> Unit,
    onShowComments: (() -> Unit)?,
    isSubscribed: Boolean,
    subscriptionInFlight: Boolean,
    onToggleSubscription: () -> Unit,
    embeddedPlayback: @Composable () -> Unit,
) {
    val mediaController = LocalMediaController.current
    val currentMediaId = rememberCurrentMediaId(mediaController)
    val branding = rememberVideoBranding(
        sourceUrl = video.url,
        title = video.title,
        thumbnailUrl = video.thumbnailUrl,
        durationSeconds = video.durationSeconds,
    )
    val overlayMotion = Modifier.graphicsLayer {
        alpha = visuals.overlayAlpha
        translationY = visuals.overlayTranslationY
    }
    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            model = branding.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (isActive) embeddedPlayback()
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.16f),
                    0.55f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.86f),
                ),
            ),
        )
        if (isActive) {
            ShortsActionRail(
                video = video,
                state = menuItemState,
                onOpenPlayer = { onPlayVideo(video.url) },
                onAction = onMenuAction,
                onShowComments = onShowComments,
                modifier = Modifier.align(Alignment.CenterEnd)
                    .padding(end = 12.dp)
                    .then(overlayMotion),
            )
        } else if (!embeddedPlaybackEnabled) {
            FilledIconButton(
                onClick = { onPlayVideo(video.url) },
                modifier = Modifier.align(Alignment.Center).size(68.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = stringResource(R.string.shorts_play, branding.title),
                    modifier = Modifier.size(38.dp),
                )
            }
        }
        ShortsInfoOverlay(
            video = video,
            title = branding.title,
            isSubscribed = isSubscribed,
            subscriptionInFlight = subscriptionInFlight,
            onOpenChannel = { onOpenChannel(video.uploaderUrl) },
            onToggleSubscription = onToggleSubscription,
            modifier = Modifier.align(Alignment.BottomStart).then(overlayMotion),
        )
        if (isActive && mediaController != null && currentMediaId == video.url) {
            ShortsPlaybackProgress(
                player = mediaController,
                fallbackDurationMs = video.durationSeconds * 1_000L,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}
