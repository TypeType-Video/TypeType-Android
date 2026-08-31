package video.typetype.tv.player

import androidx.media3.common.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal class SponsorBlockController(
    private val scope: CoroutineScope,
    private val player: Player,
    private val policy: SponsorBlockPolicy,
    private val playbackVolume: Float,
    private val onSeek: (Long) -> Unit,
) {
    private var job: Job? = null
    private var previousPosition: Long? = null
    private var pendingSegment: TvSponsorBlockSegment? = null
    private var mutedBySponsorBlock = false

    fun start() {
        stop()
        if (policy.autoSkipSegments.isEmpty()) return
        job = scope.launch {
            while (isActive) {
                processPosition(player.currentPosition.coerceAtLeast(0L))
                delay(POLL_INTERVAL_MILLISECONDS)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        previousPosition = null
        pendingSegment = null
        restoreVolume()
    }

    private fun processPosition(position: Long) {
        val previous = previousPosition
        previousPosition = position
        pendingSegment?.let { pending ->
            if (position >= pending.endMilliseconds - 100L || position < pending.startMilliseconds) {
                pendingSegment = null
            }
        }
        val active = policy.autoSkipSegments.firstOrNull {
            position in it.startMilliseconds until it.endMilliseconds
        }
        if (policy.muteInsteadOfSkip) {
            if (active != null) mute() else restoreVolume()
            return
        }
        restoreVolume()
        if (active == null || pendingSegment == active || previous == null || position <= previous) return
        val crossedStart = previous <= active.startMilliseconds + START_TOLERANCE_MILLISECONDS &&
            position >= active.startMilliseconds
        if (!crossedStart) return
        pendingSegment = active
        onSeek(policy.skipTarget(active, player.duration.coerceAtLeast(0L)))
    }

    private fun mute() {
        if (mutedBySponsorBlock) return
        mutedBySponsorBlock = true
        player.volume = 0f
    }

    private fun restoreVolume() {
        if (!mutedBySponsorBlock) return
        mutedBySponsorBlock = false
        player.volume = playbackVolume
    }
}

private const val POLL_INTERVAL_MILLISECONDS = 250L
private const val START_TOLERANCE_MILLISECONDS = 500L
