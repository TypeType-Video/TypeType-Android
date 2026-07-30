package dev.typetype.android.services

import androidx.media3.common.C
import androidx.media3.common.Player
import dev.typetype.android.domain.playback.PlaybackQueueState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal class PlaybackQueuePreloadTicker(
    private val scope: CoroutineScope,
    private val player: () -> Player?,
    private val queue: () -> PlaybackQueueState,
    private val resolvedNextUrl: () -> String?,
    private val resolvedAtMillis: () -> Long,
    private val prepareNext: (forceRefresh: Boolean) -> Unit,
) {
    private var job: Job? = null

    fun start() {
        job?.cancel()
        job = scope.launch {
            while (isActive) {
                delay(TICK_INTERVAL_MILLIS)
                val currentPlayer = player() ?: continue
                val currentQueue = queue()
                if (!currentQueue.isActive || currentQueue.next == null) continue
                val remaining = currentPlayer.remainingPlaybackMillis()
                val resolvedAt = resolvedAtMillis()
                val stale = resolvedAt > 0L &&
                    System.currentTimeMillis() - resolvedAt >= RESOLUTION_MAX_AGE_MILLIS
                when {
                    resolvedNextUrl() == null -> prepareNext(false)
                    stale && remaining <= REFRESH_WINDOW_MILLIS -> prepareNext(true)
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}

private fun Player.remainingPlaybackMillis(): Long =
    if (duration == C.TIME_UNSET || duration <= 0L) {
        Long.MAX_VALUE
    } else {
        duration - currentPosition
    }

private const val TICK_INTERVAL_MILLIS = 1_000L
private const val RESOLUTION_MAX_AGE_MILLIS = 120_000L
private const val REFRESH_WINDOW_MILLIS = 60_000L
