package dev.typetype.android.feature.player

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Singleton
internal class PlaybackPrewarmLauncher @Inject constructor(
    private val preheater: PlaybackPreheater,
) : PlaybackPrewarm {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val inFlight = mutableSetOf<String>()

    override fun prewarm(videoUrl: String) {
        if (videoUrl.isBlank()) return
        synchronized(inFlight) {
            if (!inFlight.add(videoUrl)) return
        }
        scope.launch {
            try {
                preheater.preheat(videoUrl)
            } finally {
                synchronized(inFlight) { inFlight.remove(videoUrl) }
            }
        }
    }
}
