package video.typetype.tv.player

import androidx.media3.common.Player
import org.junit.Assert.assertEquals
import org.junit.Test

public class TvPlaybackQualityMetricsTest {
    @Test
    public fun recordsStartupPlaybackAndRebuffering() {
        val metrics = TvPlaybackQualityMetrics()
        metrics.onIsPlayingChanged(true, now = 100L)
        metrics.onPlaybackStateChanged(Player.STATE_READY, now = 110L)
        metrics.onRenderedFirstFrameAt(120L)
        metrics.onPlaybackStateChanged(Player.STATE_BUFFERING, now = 300L)
        metrics.onPlaybackStateChanged(Player.STATE_READY, now = 450L)
        metrics.onIsPlayingChanged(false, now = 700L)

        val snapshot = metrics.snapshot(now = 900L)
        assertEquals(20L, snapshot.startupMilliseconds)
        assertEquals(600L, snapshot.playedMilliseconds)
        assertEquals(150L, snapshot.rebufferMilliseconds)
    }

    @Test
    public fun clampsNegativeDroppedFrameCounts() {
        val metrics = TvPlaybackQualityMetrics()
        metrics.onDroppedVideoFramesCount(-2)
        metrics.onDroppedVideoFramesCount(3)

        assertEquals(3L, metrics.snapshot(now = 100L).droppedVideoFrames)
    }
}
