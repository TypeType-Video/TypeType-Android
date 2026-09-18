package video.typetype.tv.player

import android.util.Log
import video.typetype.sdk.media3.PlaybackMediaSourceHandle
import video.typetype.sdk.media3.SabrMediaSourceHandle

internal fun logSabrMetrics(handle: PlaybackMediaSourceHandle?) {
    val metrics = (handle as? SabrMediaSourceHandle)?.metrics() ?: return
    Log.i(
        "TypeTypeSabr",
        "metrics sessions=${metrics.sessionsOpened} recoveries=${metrics.recoveries} " +
            "windows=${metrics.windowRequests} prefetch=${metrics.prefetchRequests} " +
            "segmentsEndpoint=${metrics.segmentsEndpointRequests} segmentRequests=${metrics.segmentRequests} " +
            "pending=${metrics.pendingSegments} mediaBytes=${metrics.mediaBytes}",
    )
}
