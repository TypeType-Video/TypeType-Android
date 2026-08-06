package dev.typetype.android.feature.player

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.media3.common.Player
import dev.typetype.android.domain.feed.Video
import dev.typetype.android.domain.playback.PlaybackQueueEntry
import kotlinx.coroutines.delay
import kotlin.math.ceil

internal data class AutoplayCountdownTarget(
    val videoUrl: String,
    val title: String,
    val thumbnailUrl: String,
    val channelName: String,
)

internal data class AutoplayCountdownState(
    val target: AutoplayCountdownTarget,
    val remainingSeconds: Int,
    val progress: Float,
    val paused: Boolean,
    val playNow: () -> Unit,
    val cancel: () -> Unit,
    val togglePause: () -> Unit,
)

@Composable
internal fun rememberAutoplayCountdown(
    player: Player?,
    currentVideoUrl: String,
    enabled: Boolean,
    countdownSeconds: Int,
    target: AutoplayCountdownTarget?,
    onPlayTarget: (AutoplayCountdownTarget) -> Unit,
): AutoplayCountdownState? {
    val boundedSeconds = countdownSeconds.coerceIn(0, MAX_AUTOPLAY_COUNTDOWN_SECONDS)
    val latestPlayTarget by rememberUpdatedState(onPlayTarget)
    var activeTarget by remember(currentVideoUrl) {
        mutableStateOf<AutoplayCountdownTarget?>(null)
    }
    var remainingMillis by remember(currentVideoUrl) { mutableLongStateOf(0L) }
    var paused by remember(currentVideoUrl) { mutableStateOf(false) }
    var handledTargetUrl by remember(currentVideoUrl) { mutableStateOf<String?>(null) }

    fun playNow() {
        val next = activeTarget ?: return
        activeTarget = null
        paused = false
        latestPlayTarget(next)
    }

    DisposableEffect(player, currentVideoUrl, enabled, boundedSeconds, target?.videoUrl) {
        fun handlePlaybackState(playbackState: Int) {
            if (playbackState != Player.STATE_ENDED) {
                handledTargetUrl = null
                return
            }
            if (!shouldStartAutoplayCountdown(
                    playbackState = playbackState,
                    playWhenReady = player?.playWhenReady == true,
                    enabled = enabled,
                    currentMediaId = player?.currentMediaItem?.mediaId,
                    currentVideoUrl = currentVideoUrl,
                    targetUrl = target?.videoUrl,
                )
            ) {
                if (player?.playWhenReady != true && activeTarget != null) {
                    activeTarget = null
                    paused = false
                }
                return
            }
            val next = target ?: return
            if (handledTargetUrl == next.videoUrl) return
            handledTargetUrl = next.videoUrl
            if (boundedSeconds == 0) {
                latestPlayTarget(next)
            } else {
                remainingMillis = boundedSeconds * 1_000L
                paused = false
                activeTarget = next
            }
        }
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                handlePlaybackState(playbackState)
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                handlePlaybackState(player?.playbackState ?: return)
            }
        }
        player?.addListener(listener)
        player?.playbackState?.let(::handlePlaybackState)
        onDispose {
            player?.removeListener(listener)
            if (activeTarget?.videoUrl == target?.videoUrl) {
                activeTarget = null
                paused = false
            }
            handledTargetUrl = null
        }
    }

    LaunchedEffect(activeTarget?.videoUrl, paused) {
        if (activeTarget == null || paused) return@LaunchedEffect
        val startedAt = SystemClock.elapsedRealtime()
        val startedWith = remainingMillis
        while (activeTarget != null && !paused) {
            val elapsed = SystemClock.elapsedRealtime() - startedAt
            remainingMillis = (startedWith - elapsed).coerceAtLeast(0L)
            if (remainingMillis == 0L) {
                playNow()
                break
            }
            delay(COUNTDOWN_TICK_MILLIS)
        }
    }

    val next = activeTarget ?: return null
    return AutoplayCountdownState(
        target = next,
        remainingSeconds = ceil(remainingMillis / 1_000.0).toInt(),
        progress = if (boundedSeconds == 0) {
            0f
        } else {
            remainingMillis.toFloat() / (boundedSeconds * 1_000f)
        },
        paused = paused,
        playNow = ::playNow,
        cancel = {
            activeTarget = null
            paused = false
        },
        togglePause = { paused = !paused },
    )
}

internal fun Video.toAutoplayCountdownTarget() = AutoplayCountdownTarget(
    videoUrl = url,
    title = title,
    thumbnailUrl = thumbnailUrl,
    channelName = uploaderName,
)

internal fun PlaybackQueueEntry.toAutoplayCountdownTarget() = AutoplayCountdownTarget(
    videoUrl = videoUrl,
    title = title,
    thumbnailUrl = thumbnailUrl,
    channelName = channelName,
)

internal fun shouldStartAutoplayCountdown(
    playbackState: Int,
    playWhenReady: Boolean,
    enabled: Boolean,
    currentMediaId: String?,
    currentVideoUrl: String,
    targetUrl: String?,
): Boolean = playbackState == Player.STATE_ENDED &&
    playWhenReady &&
    enabled &&
    currentMediaId == currentVideoUrl &&
    !targetUrl.isNullOrBlank()

private const val COUNTDOWN_TICK_MILLIS = 100L
private const val MAX_AUTOPLAY_COUNTDOWN_SECONDS = 60
