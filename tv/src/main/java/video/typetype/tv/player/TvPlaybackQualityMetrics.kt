@file:androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])

package video.typetype.tv.player

import android.os.SystemClock
import androidx.media3.common.Player
import androidx.media3.exoplayer.analytics.AnalyticsListener

internal data class TvPlaybackQualitySnapshot(
    val startupMilliseconds: Long?,
    val playedMilliseconds: Long,
    val rebufferMilliseconds: Long,
    val droppedVideoFrames: Long,
)

internal class TvPlaybackQualityMetrics : AnalyticsListener {
    private var playStartedAt = UNSET
    private var firstFrameAt = UNSET
    private var playingSince = UNSET
    private var rebufferingSince = UNSET
    private var playedMilliseconds = 0L
    private var rebufferMilliseconds = 0L
    private var droppedVideoFrames = 0L

    fun onPlaybackRequested(now: Long = SystemClock.elapsedRealtime()) {
        playStartedAt = now
        firstFrameAt = UNSET
        playingSince = UNSET
        rebufferingSince = UNSET
        playedMilliseconds = 0L
        rebufferMilliseconds = 0L
        droppedVideoFrames = 0L
    }

    fun onPlaybackStateChanged(state: Int, now: Long = SystemClock.elapsedRealtime()) {
        if (firstFrameAt == UNSET) return
        if (state == Player.STATE_BUFFERING && rebufferingSince == UNSET) {
            rebufferingSince = now
        } else if (state != Player.STATE_BUFFERING && rebufferingSince != UNSET) {
            rebufferMilliseconds += (now - rebufferingSince).coerceAtLeast(0L)
            rebufferingSince = UNSET
        }
    }

    fun onIsPlayingChanged(isPlaying: Boolean, now: Long = SystemClock.elapsedRealtime()) {
        if (isPlaying) {
            if (playStartedAt == UNSET) playStartedAt = now
            if (playingSince == UNSET) playingSince = now
        } else if (playingSince != UNSET) {
            playedMilliseconds += (now - playingSince).coerceAtLeast(0L)
            playingSince = UNSET
        }
    }

    override fun onRenderedFirstFrame(
        eventTime: AnalyticsListener.EventTime,
        output: Any,
        renderTimeMs: Long,
    ) {
        onRenderedFirstFrameAt(eventTime.realtimeMs)
    }

    fun onRenderedFirstFrameAt(now: Long) {
        if (firstFrameAt == UNSET) firstFrameAt = now
    }

    override fun onDroppedVideoFrames(
        eventTime: AnalyticsListener.EventTime,
        droppedFrames: Int,
        elapsedMs: Long,
    ) {
        onDroppedVideoFramesCount(droppedFrames)
    }

    fun onDroppedVideoFramesCount(droppedFrames: Int) {
        droppedVideoFrames += droppedFrames.coerceAtLeast(0).toLong()
    }

    fun snapshot(now: Long = SystemClock.elapsedRealtime()): TvPlaybackQualitySnapshot {
        val played = playedMilliseconds + if (playingSince == UNSET) 0L else {
            (now - playingSince).coerceAtLeast(0L)
        }
        val rebuffered = rebufferMilliseconds + if (rebufferingSince == UNSET) 0L else {
            (now - rebufferingSince).coerceAtLeast(0L)
        }
        return TvPlaybackQualitySnapshot(
            startupMilliseconds = if (playStartedAt == UNSET || firstFrameAt == UNSET) null
            else (firstFrameAt - playStartedAt).coerceAtLeast(0L),
            playedMilliseconds = played,
            rebufferMilliseconds = rebuffered,
            droppedVideoFrames = droppedVideoFrames,
        )
    }

    private companion object {
        const val UNSET = -1L
    }
}
