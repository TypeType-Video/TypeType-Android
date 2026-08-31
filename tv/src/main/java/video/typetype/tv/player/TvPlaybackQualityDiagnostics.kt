package video.typetype.tv.player

import android.util.Log

internal fun logTvPlaybackQuality(metrics: TvPlaybackQualityMetrics) {
    val snapshot = metrics.snapshot()
    Log.i(
        "TypeTypePlayback",
        "quality startupMs=${snapshot.startupMilliseconds ?: -1L} " +
            "playedMs=${snapshot.playedMilliseconds} rebufferMs=${snapshot.rebufferMilliseconds} " +
            "droppedVideoFrames=${snapshot.droppedVideoFrames} " +
            "renderedVideoFrames=${snapshot.renderedVideoFrames}",
    )
}
