package dev.typetype.android.services

import androidx.media3.common.MediaItem
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.analytics.PlayerId
import androidx.media3.exoplayer.source.TrackGroupArray
import androidx.media3.exoplayer.trackselection.ExoTrackSelection
import androidx.media3.exoplayer.upstream.Allocator
import androidx.media3.exoplayer.source.MediaSource
import dev.typetype.android.domain.preferences.DEFAULT_YOUTUBE_BUFFER_SECONDS
import dev.typetype.android.domain.preferences.youTubeBufferGoalMs

@UnstableApi
internal class YouTubeBufferLoadControl(
    private val delegate: DefaultLoadControl,
    private val policy: PlaybackBufferPolicy,
) : LoadControl {
    private val loading = mutableMapOf<PlayerId, Boolean>()
    private val period = Timeline.Period()
    private val window = Timeline.Window()

    // Java interface defaults are not forwarded by Kotlin interface delegation.
    // Explicitly forward every Media3 callback used by ExoPlayer.
    override fun getAllocator(playerId: PlayerId): Allocator = delegate.getAllocator(playerId)

    override fun getBackBufferDurationUs(playerId: PlayerId): Long =
        delegate.getBackBufferDurationUs(playerId)

    override fun retainBackBufferFromKeyframe(playerId: PlayerId): Boolean =
        delegate.retainBackBufferFromKeyframe(playerId)

    override fun onTracksSelected(
        parameters: LoadControl.Parameters,
        trackGroups: TrackGroupArray,
        trackSelections: Array<out ExoTrackSelection?>,
    ) = delegate.onTracksSelected(parameters, trackGroups, trackSelections)

    override fun shouldStartPlayback(parameters: LoadControl.Parameters): Boolean =
        delegate.shouldStartPlayback(parameters)

    override fun onPrepared(playerId: PlayerId) {
        loading.remove(playerId)
        delegate.onPrepared(playerId)
    }

    override fun onStopped(playerId: PlayerId) {
        loading.remove(playerId)
        delegate.onStopped(playerId)
    }

    override fun onReleased(playerId: PlayerId) {
        loading.remove(playerId)
        delegate.onReleased(playerId)
    }

    override fun shouldContinueLoading(parameters: LoadControl.Parameters): Boolean {
        if (parameters.playerId == PlayerId.PRELOAD) return delegate.shouldContinueLoading(parameters)
        val windowIndex = parameters.timeline
            .getPeriodByUid(parameters.mediaPeriodId.periodUid, period).windowIndex
        val item = parameters.timeline.getWindow(windowIndex, window).mediaItem
        val seconds = item.youTubeBufferSeconds()
        if (seconds == null) {
            loading.remove(parameters.playerId)
            return delegate.shouldContinueLoading(parameters)
        }
        val goalUs = youTubeBufferGoalMs(seconds, parameters.playbackSpeed) * 1_000L
        val keepLoading = shouldLoadYouTubeBuffer(
            bufferedDurationUs = parameters.bufferedDurationUs,
            goalUs = goalUs,
            allocatedBytes = delegate.getAllocator(parameters.playerId).totalBytesAllocated,
            budgetBytes = policy.targetBufferBytes,
            wasLoading = loading[parameters.playerId] ?: false,
        )
        loading[parameters.playerId] = keepLoading
        return keepLoading
    }

    override fun shouldContinuePreloading(
        playerId: PlayerId,
        timeline: Timeline,
        mediaPeriodId: MediaSource.MediaPeriodId,
        bufferedDurationUs: Long,
    ): Boolean = loading.values.none { it } && delegate.shouldContinuePreloading(
        playerId, timeline, mediaPeriodId, bufferedDurationUs,
    )
}

internal fun MediaItem.youTubeBufferSeconds(): Int? {
    val extras = requestMetadata.extras ?: return null
    if (extras.getString(MergedStreamMediaKeys.EXTRA_SABR_REQUEST_KEY).isNullOrBlank()) return null
    if (extras.getBoolean(MergedStreamMediaKeys.EXTRA_SABR_IS_LIVE) ||
        extras.getBoolean(MergedStreamMediaKeys.EXTRA_SABR_LIVE_ACTIVE)
    ) return null
    return extras.getInt(MergedStreamMediaKeys.EXTRA_YOUTUBE_BUFFER_SECONDS, DEFAULT_YOUTUBE_BUFFER_SECONDS)
}

internal fun shouldLoadYouTubeBuffer(
    bufferedDurationUs: Long,
    goalUs: Long,
    allocatedBytes: Int,
    budgetBytes: Int,
    wasLoading: Boolean,
): Boolean = when {
    allocatedBytes >= budgetBytes -> false
    bufferedDurationUs < goalUs * 2 / 3 -> true
    bufferedDurationUs >= goalUs -> false
    else -> wasLoading
}
