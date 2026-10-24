package dev.typetype.android.feature.shorts

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.typetype.android.R
import dev.typetype.android.core.ui.branding.rememberVideoBranding
import dev.typetype.android.core.ui.components.VideoMenuAction
import dev.typetype.android.core.ui.components.VideoMenuItemState
import dev.typetype.android.core.ui.share.LocalServerBaseUrl
import dev.typetype.android.core.ui.share.buildImageUrl
import dev.typetype.android.domain.feed.Video
import dev.typetype.android.feature.player.ShortsPlaybackProgress
import dev.typetype.android.feature.player.components.LocalMediaController
import dev.typetype.android.feature.player.components.rememberCurrentMediaId
import kotlin.math.absoluteValue

internal fun shortsOverlayAlpha(pageOffset: Float): Float =
    1f - pageOffset.coerceIn(0f, 1f) * 0.62f

internal fun shortsOverlayTranslationY(pageOffset: Float): Float =
    pageOffset.coerceIn(0f, 1f) * 24f

@Composable
internal fun ShortPage(
    video: Video,
    isActive: Boolean,
    embeddedPlaybackEnabled: Boolean,
    enhanceBranding: Boolean,
    overlayMotion: Modifier,
    onPlayVideo: (String) -> Unit,
    onOpenChannel: (String) -> Unit,
    menuItemState: VideoMenuItemState,
    stats: ShortsVideoStats,
    onMenuAction: (VideoMenuAction) -> Unit,
    onShowComments: (() -> Unit)?,
    onCopyTitle: (String) -> Unit,
    isSubscribed: Boolean,
    subscriptionInFlight: Boolean,
    onToggleSubscription: () -> Unit,
    embeddedPlayback: @Composable () -> Unit,
) {
    val mediaController = LocalMediaController.current
    val serverBaseUrl = LocalServerBaseUrl.current
    val hapticFeedback = LocalHapticFeedback.current
    val currentMediaId = rememberCurrentMediaId(mediaController)
    val branding = rememberVideoBranding(
        sourceUrl = video.url,
        title = video.title,
        thumbnailUrl = video.thumbnailUrl,
        durationSeconds = video.durationSeconds,
        loadEnhancements = enhanceBranding,
    )
    Box(
        modifier = Modifier.fillMaxSize()
            .pointerInput(video.id, video.uploaderUrl) {
                awaitEachGesture {
                    val down = awaitFirstDown(
                        requireUnconsumed = false,
                        pass = PointerEventPass.Initial,
                    )
                    var horizontalDrag = 0f
                    var verticalDrag = 0f
                    do {
                        val change = awaitPointerEvent(PointerEventPass.Initial).changes
                            .firstOrNull { it.id == down.id } ?: break
                        val delta = change.positionChange()
                        horizontalDrag += delta.x
                        verticalDrag += delta.y
                    } while (change.pressed)
                    if (
                        horizontalDrag <= -SHORTS_CHANNEL_SWIPE_THRESHOLD_PX &&
                        horizontalDrag.absoluteValue > verticalDrag.absoluteValue
                    ) {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onOpenChannel(video.uploaderUrl)
                    }
                }
            },
    ) {
        AsyncImage(
            model = buildImageUrl(serverBaseUrl, branding.thumbnailUrl),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (isActive) embeddedPlayback()
        Box(
            modifier = Modifier.fillMaxSize()
                .background(
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
            stats = stats,
            isSubscribed = isSubscribed,
            subscriptionInFlight = subscriptionInFlight,
            onOpenChannel = { onOpenChannel(video.uploaderUrl) },
            onCopyTitle = onCopyTitle,
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

private const val SHORTS_CHANNEL_SWIPE_THRESHOLD_PX = 96f
