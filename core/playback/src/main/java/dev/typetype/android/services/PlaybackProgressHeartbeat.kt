package dev.typetype.android.services

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class PlaybackProgressHeartbeat(
    private val scope: CoroutineScope,
    private val intervalMillis: Long,
    private val wait: suspend (Long) -> Unit = { delay(it) },
    private val report: () -> Unit,
) {
    private var job: Job? = null

    fun onPlayingChanged(isPlaying: Boolean) {
        if (!isPlaying) {
            stop()
            return
        }
        if (job?.isActive == true) return
        job = scope.launch {
            while (true) {
                wait(intervalMillis)
                report()
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
