package dev.typetype.android.feature.player

import android.app.Activity
import android.graphics.Rect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import dev.typetype.android.feature.player.components.applyAutoEnterPipParams
import kotlinx.coroutines.delay

@Composable
internal fun PlayerProgressEffects(
    controller: MediaController?,
    activity: Activity?,
    durationMillis: Long,
    pipSourceRect: Rect?,
    onSaveProgress: (Long) -> Unit,
) {
    val saveIfEligible: (Long) -> Unit = save@{ positionMillis ->
        if (positionMillis < MIN_PROGRESS_MILLIS) return@save
        if (durationMillis > 0 && positionMillis >= (durationMillis * MAX_PROGRESS_FRACTION).toLong()) return@save
        onSaveProgress(positionMillis)
    }

    LaunchedEffect(controller) {
        while (true) {
            delay(PROGRESS_INTERVAL_MILLIS)
            val current = controller ?: continue
            if (current.isPlaying) saveIfEligible(current.currentPosition)
        }
    }

    DisposableEffect(controller, pipSourceRect) {
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
                        sourceRect = pipSourceRect,
                    )
                    if (!isPlaying) saveIfEligible(current.currentPosition)
                }

                override fun onPositionDiscontinuity(
                    oldPosition: Player.PositionInfo,
                    newPosition: Player.PositionInfo,
                    reason: Int,
                ) {
                    if (reason == Player.DISCONTINUITY_REASON_SEEK) {
                        saveIfEligible(newPosition.positionMs)
                    }
                }
            }
            current.addListener(listener)
            applyAutoEnterPipParams(
                activity,
                autoEnter = current.isPlaying,
                isPlaying = current.isPlaying,
                sourceRect = pipSourceRect,
            )
            onDispose {
                saveIfEligible(current.currentPosition)
                current.removeListener(listener)
                applyAutoEnterPipParams(activity, false)
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, controller) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                controller?.let { saveIfEligible(it.currentPosition) }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}

private const val MIN_PROGRESS_MILLIS = 5_000L
private const val MAX_PROGRESS_FRACTION = 0.95
private const val PROGRESS_INTERVAL_MILLIS = 10_000L
