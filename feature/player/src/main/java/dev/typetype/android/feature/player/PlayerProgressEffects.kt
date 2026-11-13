package dev.typetype.android.feature.player

import android.app.Activity
import android.graphics.Rect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import dev.typetype.android.feature.player.components.applyAutoEnterPipParams
import kotlinx.coroutines.delay

@Composable
internal fun PlayerProgressEffects(
    controller: MediaController?,
    activity: Activity?,
    durationMillis: Long,
    audioOnlyAvailable: Boolean,
    pipSourceRect: Rect?,
    onSaveProgress: (videoUrl: String, positionMillis: Long) -> Unit,
) {
    val latestSave by rememberUpdatedState(onSaveProgress)
    val latestDuration by rememberUpdatedState(durationMillis)
    // The controller outlives a video change, so the position and the video it belongs to are always read from
    // the same player state; reading the screen's current URL here would file the old position under the new video.
    val saveCurrent: (Player) -> Unit = { player ->
        val playerDuration = player.duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: latestDuration
        playerProgressSample(player.currentMediaItem, player.currentPosition, playerDuration)
            ?.let { latestSave(it.videoUrl, it.positionMillis) }
    }

    LaunchedEffect(controller) {
        while (true) {
            delay(PROGRESS_INTERVAL_MILLIS)
            val current = controller ?: continue
            if (current.isPlaying) saveCurrent(current)
        }
    }

    DisposableEffect(controller, audioOnlyAvailable, pipSourceRect) {
        val current = controller
        if (current == null) {
            onDispose { }
        } else {
            val listener = object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    applyAutoEnterPipParams(
                        activity,
                        autoEnter = isPlaying,
                        isPlaying = isPlaying,
                        audioOnlyAvailable = audioOnlyAvailable,
                        sourceRect = pipSourceRect,
                    )
                    if (!isPlaying) saveCurrent(current)
                }

                override fun onPositionDiscontinuity(
                    oldPosition: Player.PositionInfo,
                    newPosition: Player.PositionInfo,
                    reason: Int,
                ) {
                    discontinuityProgressSample(oldPosition, newPosition, reason, latestDuration)
                        ?.let { latestSave(it.videoUrl, it.positionMillis) }
                }
            }
            current.addListener(listener)
            applyAutoEnterPipParams(
                activity,
                autoEnter = current.isPlaying,
                isPlaying = current.isPlaying,
                audioOnlyAvailable = audioOnlyAvailable,
                sourceRect = pipSourceRect,
            )
            onDispose {
                saveCurrent(current)
                current.removeListener(listener)
                applyAutoEnterPipParams(activity, false)
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, controller) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                controller?.let(saveCurrent)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}

private const val PROGRESS_INTERVAL_MILLIS = 10_000L
