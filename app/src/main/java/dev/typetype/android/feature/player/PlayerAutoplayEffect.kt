package dev.typetype.android.feature.player

import androidx.compose.runtime.Composable
import androidx.media3.common.Player
import dev.typetype.android.domain.feed.Video
import dev.typetype.android.domain.playback.PlaybackQueueState
import dev.typetype.android.domain.stream.Stream
import kotlin.math.ceil

@Composable
internal fun rememberPlayerAutoplayCountdown(
    player: Player?,
    stream: Stream,
    playbackQueue: PlaybackQueueState,
    enabled: Boolean,
    countdownSeconds: Int,
    onAdvanceQueue: () -> Unit,
    onCancelQueueAutoplay: () -> Unit,
    onToggleQueueAutoplayPause: () -> Unit,
    onPlayVideo: (String) -> Unit,
): AutoplayCountdownState? {
    if (playbackQueue.isActive) {
        return queueAutoplayCountdownState(
            playbackQueue = playbackQueue,
            onAdvanceQueue = onAdvanceQueue,
            onCancel = onCancelQueueAutoplay,
            onTogglePause = onToggleQueueAutoplayPause,
        )
    }
    val target = selectAutoplayTarget(playbackQueue, stream.relatedStreams.firstOrNull())
    return rememberAutoplayCountdown(
        player = player,
        enabled = enabled,
        countdownSeconds = countdownSeconds,
        target = target,
        onPlayTarget = { target -> onPlayVideo(target.videoUrl) },
    )
}

internal fun selectAutoplayTarget(
    playbackQueue: PlaybackQueueState,
    relatedVideo: Video?,
): AutoplayCountdownTarget? =
    relatedVideo?.takeUnless { playbackQueue.isActive }?.toAutoplayCountdownTarget()

internal fun queueAutoplayCountdownState(
    playbackQueue: PlaybackQueueState,
    onAdvanceQueue: () -> Unit,
    onCancel: () -> Unit,
    onTogglePause: () -> Unit,
): AutoplayCountdownState? {
    val countdown = playbackQueue.autoplayCountdown ?: return null
    val target = playbackQueue.next
        ?.takeIf { it.videoUrl == countdown.targetVideoUrl }
        ?.toAutoplayCountdownTarget()
        ?: return null
    return AutoplayCountdownState(
        target = target,
        remainingSeconds = ceil(countdown.remainingMillis / 1_000.0).toInt(),
        progress = if (countdown.totalMillis == 0L) {
            0f
        } else {
            countdown.remainingMillis.toFloat() / countdown.totalMillis
        },
        paused = countdown.paused,
        playNow = onAdvanceQueue,
        cancel = onCancel,
        togglePause = onTogglePause,
    )
}
