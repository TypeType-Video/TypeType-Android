package dev.typetype.android.feature.player.components

import android.graphics.Color
import android.view.LayoutInflater
import androidx.annotation.OptIn
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import dev.typetype.android.R
import dev.typetype.android.feature.player.state.ResizeMode
import kotlin.math.max

@OptIn(markerClass = [UnstableApi::class])
@Composable
internal fun ResilientPlayerSurface(
    player: Player,
    surfaceKey: String,
    resizeMode: ResizeMode,
    modifier: Modifier = Modifier,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var videoAspectRatio by remember(player) { mutableFloatStateOf(player.videoSize.aspectRatio()) }
    var view by remember(surfaceKey) { mutableStateOf<PlayerView?>(null) }
    BoxWithConstraints(modifier = modifier.clipToBounds()) {
        val targetScale = resizeMode.toTargetScale(
            videoAspectRatio = videoAspectRatio,
            containerAspectRatio = constraints.containerAspectRatio(),
        )
        val scaleX by animateFloatAsState(
            targetValue = targetScale.x,
            animationSpec = tween(durationMillis = RESIZE_ANIMATION_MS, easing = FastOutSlowInEasing),
            label = "playerSurfaceScaleX",
        )
        val scaleY by animateFloatAsState(
            targetValue = targetScale.y,
            animationSpec = tween(durationMillis = RESIZE_ANIMATION_MS, easing = FastOutSlowInEasing),
            label = "playerSurfaceScaleY",
        )
        key(surfaceKey) {
            AndroidView(
                factory = { context ->
                    (LayoutInflater.from(context).inflate(R.layout.player_view_texture, null) as PlayerView).apply {
                        useController = false
                        setShutterBackgroundColor(Color.BLACK)
                        this.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        this.player = player
                        view = this
                        onResume()
                    }
                },
                update = { playerView ->
                    playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    if (playerView.player !== player) {
                        playerView.player = player
                    }
                    playerView.onResume()
                },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        this.scaleX = scaleX
                        this.scaleY = scaleY
                        transformOrigin = TransformOrigin.Center
                    },
            )
        }
    }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                videoAspectRatio = videoSize.aspectRatio()
            }
        }
        player.addListener(listener)
        videoAspectRatio = player.videoSize.aspectRatio()
        onDispose { player.removeListener(listener) }
    }
    DisposableEffect(lifecycleOwner, player, view) {
        val playerView = view
        if (playerView == null) {
            onDispose { }
        } else {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START,
                    Lifecycle.Event.ON_RESUME,
                    -> {
                        if (playerView.player !== player) {
                            playerView.player = player
                        }
                        playerView.onResume()
                    }
                    Lifecycle.Event.ON_PAUSE,
                    Lifecycle.Event.ON_STOP,
                    -> if (!player.isPlaying) {
                        playerView.onPause()
                    }
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                playerView.player = null
                playerView.onPause()
            }
        }
    }
}

private data class SurfaceScale(
    val x: Float,
    val y: Float,
)

private fun ResizeMode.toTargetScale(
    videoAspectRatio: Float,
    containerAspectRatio: Float,
): SurfaceScale {
    if (videoAspectRatio <= 0f || containerAspectRatio <= 0f) return SurfaceScale(1f, 1f)
    val horizontalFill = if (videoAspectRatio < containerAspectRatio) {
        containerAspectRatio / videoAspectRatio
    } else {
        1f
    }
    val verticalFill = if (videoAspectRatio > containerAspectRatio) {
        videoAspectRatio / containerAspectRatio
    } else {
        1f
    }
    return when (this) {
        ResizeMode.Fit -> SurfaceScale(1f, 1f)
        ResizeMode.Crop -> {
            val scale = max(horizontalFill, verticalFill)
            SurfaceScale(scale, scale)
        }
        ResizeMode.Stretch -> SurfaceScale(horizontalFill, verticalFill)
    }
}

private fun Constraints.containerAspectRatio(): Float =
    if (maxWidth > 0 && maxHeight > 0) maxWidth.toFloat() / maxHeight.toFloat() else 0f

private fun VideoSize.aspectRatio(): Float =
    if (width > 0 && height > 0) width * pixelWidthHeightRatio / height else 0f

private const val RESIZE_ANIMATION_MS = 220
