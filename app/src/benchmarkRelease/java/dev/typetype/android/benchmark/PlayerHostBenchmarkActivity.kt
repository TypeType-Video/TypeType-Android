package dev.typetype.android.benchmark

import android.os.Bundle
import android.os.Looper
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
                        onTargetSettled = {
                            target = it
                            requestStamp += 1
                        },
                        onProgressChange = {},
                        miniContent = { Text("Benchmark mini player") },
                        expandedContent = { transition ->
                            PlayerContentLayout(
                                isFullscreen = false,
                                hostTransitionProgress = transition.progress,
                                modifier = Modifier.fillMaxSize(),
                                viewport = { modifier ->
                                    AndroidView(
                                        factory = { context ->
                                            PlayerView(context).apply {
                                                useController = false
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
    override fun getState(): State = State.Builder()
        .setAvailableCommands(Player.Commands.Builder().addAllCommands().build())
        .setPlaybackState(Player.STATE_READY)
        .setPlayWhenReady(true, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
        .setPlaylist(
            listOf(
                MediaItemData.Builder("benchmark-surface")
                    .setMediaItem(MediaItem.Builder().setMediaId("benchmark-surface").build())
                    .build(),
            ),
        )
        .setContentPositionMs(42_000L)
        .build()

    override fun handleSetVideoOutput(videoOutput: Any): ListenableFuture<*> =
        Futures.immediateVoidFuture()

    override fun handleClearVideoOutput(videoOutput: Any?): ListenableFuture<*> =
        Futures.immediateVoidFuture()

    override fun handleRelease(): ListenableFuture<*> = Futures.immediateVoidFuture()
}
