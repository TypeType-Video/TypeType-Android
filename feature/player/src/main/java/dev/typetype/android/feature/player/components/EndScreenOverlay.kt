package dev.typetype.android.feature.player.components

import android.content.Intent
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import dev.typetype.android.core.ui.components.ExternalLinkDialog
import dev.typetype.android.domain.stream.EndScreenElement
import dev.typetype.android.domain.stream.EndScreenElementType
import dev.typetype.android.domain.stream.boxIn
import dev.typetype.android.domain.stream.visibleAt
import dev.typetype.android.feature.player.EndScreenActions
import dev.typetype.android.feature.player.endScreenFrame
import dev.typetype.android.feature.player.state.ResizeMode
import kotlinx.coroutines.delay

@Composable
internal fun EndScreenOverlay(
    player: Player,
    elements: List<EndScreenElement>,
    resizeMode: ResizeMode,
    enabled: Boolean,
    actions: EndScreenActions,
    modifier: Modifier = Modifier,
) {
    if (elements.isEmpty()) return
    val context = LocalContext.current
    var shown by remember(elements) { mutableStateOf(emptyList<EndScreenElement>()) }
    var pendingUrl by remember(elements) { mutableStateOf<String?>(null) }
    LaunchedEffect(player, elements, enabled) {
        if (!enabled) {
            shown = emptyList()
            return@LaunchedEffect
        }
        while (true) {
            val next = elements.visibleAt(player.currentPosition)
            if (next != shown) shown = next
            delay(END_SCREEN_POLL_INTERVAL_MS)
        }
    }

    if (shown.isNotEmpty()) {
        val videoAspectRatio = rememberVideoAspectRatio(player)
        BoxWithConstraints(modifier = modifier.clipToBounds()) {
            val frame = endScreenFrame(maxWidth.value, maxHeight.value, videoAspectRatio, resizeMode)
            shown.forEach { element ->
                key(element.id) {
                    val box = element.boxIn(frame.width / frame.height)
                    EndScreenCard(
                        element = element,
                        onClick = {
                            when (element.type) {
                                EndScreenElementType.Video -> actions.onOpenVideo(element.url)
                                EndScreenElementType.Playlist -> actions.onOpenPlaylist(element.url)
                                EndScreenElementType.Channel -> actions.onOpenChannel(element.url)
                                EndScreenElementType.Website -> pendingUrl = element.url
                            }
                        },
                        modifier = Modifier
                            .offset(
                                x = (frame.left + box.left * frame.width).dp,
                                y = (frame.top + box.top * frame.height).dp,
                            )
                            .size(
                                width = (box.width * frame.width).dp,
                                height = (box.height * frame.height).dp,
                            ),
                    )
                }
            }
        }
    }

    pendingUrl?.let { url ->
        ExternalLinkDialog(
            url = url,
            onConfirm = {
                context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                pendingUrl = null
            },
            onDismiss = { pendingUrl = null },
        )
    }
}

@Composable
private fun rememberVideoAspectRatio(player: Player): Float {
    var aspectRatio by remember(player) { mutableFloatStateOf(player.videoSize.aspectRatioOrDefault()) }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                aspectRatio = videoSize.aspectRatioOrDefault()
            }
        }
        player.addListener(listener)
        aspectRatio = player.videoSize.aspectRatioOrDefault()
        onDispose { player.removeListener(listener) }
    }
    return aspectRatio
}

private fun VideoSize.aspectRatioOrDefault(): Float =
    if (width > 0 && height > 0) width * pixelWidthHeightRatio / height else DEFAULT_VIDEO_ASPECT_RATIO

private const val END_SCREEN_POLL_INTERVAL_MS = 250L
private const val DEFAULT_VIDEO_ASPECT_RATIO = 16f / 9f
