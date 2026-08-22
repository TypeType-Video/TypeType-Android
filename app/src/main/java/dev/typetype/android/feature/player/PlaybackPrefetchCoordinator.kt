package dev.typetype.android.feature.player

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal class PlaybackPrefetchCoordinator(
    private val scope: CoroutineScope,
    private val prefetch: suspend (String) -> Unit,
) {
    private val jobs = mutableMapOf<String, Job>()

    fun schedule(url: String) {
        val job = scope.launch(start = CoroutineStart.LAZY) { prefetch(url) }
        val scheduled = synchronized(jobs) {
            if (jobs[url]?.isActive == true) false else {
                jobs[url] = job
                true
            }
        }
        if (!scheduled) {
            job.cancel()
            return
        }
        job.invokeOnCompletion {
            synchronized(jobs) {
                if (jobs[url] === job) jobs.remove(url)
            }
        }
        job.start()
    }

    suspend fun await(url: String) {
        synchronized(jobs) { jobs[url] }?.join()
    }
}
