package dev.typetype.android.benchmark

import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.VideoSize
import androidx.media3.ui.PlayerView
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dev.typetype.android.core.ui.theme.TypeTypeTheme
import dev.typetype.android.feature.player.PlayerContentLayout
import dev.typetype.android.feature.player.host.PlayerHostMotionLayout
import dev.typetype.android.feature.player.host.PlayerHostTarget

class PlayerHostBenchmarkActivity : ComponentActivity() {
    private val player by lazy { BenchmarkSurfacePlayer(Looper.getMainLooper()) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var target by mutableStateOf(PlayerHostTarget.Expanded)
            var isFullscreen by mutableStateOf(true)
            var requestStamp by mutableLongStateOf(0L)
            TypeTypeTheme {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val density = LocalDensity.current
                    val containerHeightPx = constraints.maxHeight.toFloat()
                    val miniHeightPx = with(density) { MINI_HEIGHT.toPx() }
                    PlayerHostMotionLayout(
                        target = target,
                        requestStamp = requestStamp,
                        miniAnchorPx = containerHeightPx - miniHeightPx,
                        containerHeightPx = containerHeightPx,
                        miniHeightPx = miniHeightPx,
                        dragEnabled = true,
                        miniContentEnabled = true,
                        fullscreenCenterDragEnabled = isFullscreen,
                        onTargetSettled = {
                            target = it
                            if (it == PlayerHostTarget.Mini) isFullscreen = false
                            requestStamp += 1
                        },
                        onProgressChange = {},
                        miniContent = { Text("Benchmark mini player") },
                        expandedContent = { transition ->
                            PlayerContentLayout(
                                isFullscreen = isFullscreen,
                                hostTransitionProgress = transition.progress,
                                modifier = Modifier.fillMaxSize(),
                                viewport = { modifier ->
                                    AndroidView(
                                        factory = { context ->
                                            PlayerView(context).apply {
                                                useController = false
                                                setShutterBackgroundColor(AndroidColor.TRANSPARENT)
                                                setKeepContentOnPlayerReset(true)
                                                this.player = this@PlayerHostBenchmarkActivity.player
                                            }
                                        },
                                        modifier = modifier.background(Color.Black),
                                    )
                                },
                                details = { Box(it.requiredHeight(360.dp)) },
                            )
                        },
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        player.release()
        super.onDestroy()
    }

    private companion object {
        val MINI_HEIGHT = 64.dp
    }
}

private class BenchmarkSurfacePlayer(looper: Looper) : SimpleBasePlayer(looper) {
    private val handler = Handler(looper)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var outputSurface: Surface? = null
    private var renderedFrame = 0
    private var surfaceGeneration = 0
    private val renderFrame = object : Runnable {
        override fun run() {
            val surface = outputSurface ?: return
            if (!surface.isValid) {
                handler.postDelayed(this, FRAME_DELAY_MS)
                return
            }
            val canvas = surface.lockCanvas(null)
            try {
                val phase = renderedFrame % FRAME_PERIOD
                canvas.drawColor(AndroidColor.rgb(12, 18, 30))
                paint.color = AndroidColor.rgb(83, 109, 254)
                val left = canvas.width * phase.toFloat() / FRAME_PERIOD
                canvas.drawRect(left, 0f, left + canvas.width / 4f, canvas.height.toFloat(), paint)
                paint.color = AndroidColor.WHITE
                paint.textSize = canvas.height.coerceAtMost(canvas.width) / 5f
                canvas.drawText(
                    "$surfaceGeneration:$renderedFrame",
                    32f,
                    canvas.height * 0.72f,
                    paint,
                )
            } finally {
                surface.unlockCanvasAndPost(canvas)
            }
            renderedFrame += 1
            handler.postDelayed(this, FRAME_DELAY_MS)
        }
    }

    override fun getState(): State = State.Builder()
        .setAvailableCommands(Player.Commands.Builder().addAllCommands().build())
        .setPlaybackState(Player.STATE_READY)
        .setPlayWhenReady(true, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
        .setVideoSize(VideoSize(1920, 1080))
        .setNewlyRenderedFirstFrame(true)
        .setPlaylist(
            listOf(
                MediaItemData.Builder("benchmark-surface")
                    .setMediaItem(MediaItem.Builder().setMediaId("benchmark-surface").build())
                    .build(),
            ),
        )
        .setContentPositionMs(42_000L)
        .build()

    override fun handleSetVideoOutput(videoOutput: Any): ListenableFuture<*> {
        surfaceGeneration += 1
        outputSurface = when (videoOutput) {
            is Surface -> videoOutput
            is SurfaceHolder -> videoOutput.surface
            is SurfaceView -> videoOutput.holder.surface
            else -> null
        }
        handler.removeCallbacks(renderFrame)
        handler.post(renderFrame)
        return Futures.immediateVoidFuture()
    }

    override fun handleClearVideoOutput(videoOutput: Any?): ListenableFuture<*> {
        handler.removeCallbacks(renderFrame)
        outputSurface = null
        return Futures.immediateVoidFuture()
    }

    override fun handleRelease(): ListenableFuture<*> {
        handler.removeCallbacks(renderFrame)
        outputSurface = null
        return Futures.immediateVoidFuture()
    }

    private companion object {
        const val FRAME_DELAY_MS = 33L
        const val FRAME_PERIOD = 90
    }
}
