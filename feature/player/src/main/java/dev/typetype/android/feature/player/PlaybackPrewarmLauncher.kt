package dev.typetype.android.feature.player

import dev.typetype.android.core.ui.prewarm.PlaybackPrewarm
import dev.typetype.android.core.ui.prewarm.PressedSession
import dev.typetype.android.core.ui.prewarm.PrewarmRequest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Singleton
class PlaybackPrewarmLauncher @Inject constructor(
    private val preheater: PlaybackPreheater,
) : PlaybackPrewarm {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val lock = Any()
    private var streamJob: Job? = null
    private var pendingPress: LaunchedPress? = null

    override fun prefetchStreams(requests: List<PrewarmRequest>) {
        val playable = requests.filter { it.isPreparable() }
        if (playable.isEmpty()) return
        val job = scope.launch {
            playable.forEach { preheater.prefetchStream(it.videoUrl) }
        }
        synchronized(lock) {
            streamJob?.cancel()
            streamJob = job
        }
    }

    override fun cancelStreamPrefetch() {
        synchronized(lock) {
            streamJob?.cancel()
            streamJob = null
        }
    }

    override fun pressSession(request: PrewarmRequest): PressedSession {
        if (!request.isPreparable()) return IgnoredPress
        val confirmed = CompletableDeferred<Unit>()
        val press = LaunchedPress(
            job = scope.launch { preheater.preheat(request.videoUrl) { confirmed.await() } },
            confirmed = confirmed,
        )
        synchronized(lock) {
            pendingPress?.cancelUnlessConfirmed()
            pendingPress = press
        }
        return press
    }

    private class LaunchedPress(
        private val job: Job,
        private val confirmed: CompletableDeferred<Unit>,
    ) : PressedSession {
        override fun confirm() {
            confirmed.complete(Unit)
        }

        override fun cancel() = cancelUnlessConfirmed()

        fun cancelUnlessConfirmed() {
            if (!confirmed.isCompleted) job.cancel()
        }
    }

    private object IgnoredPress : PressedSession {
        override fun confirm() = Unit

        override fun cancel() = Unit
    }
}

private fun PrewarmRequest.isPreparable(): Boolean = !knownLive && videoUrl.isNotBlank()
