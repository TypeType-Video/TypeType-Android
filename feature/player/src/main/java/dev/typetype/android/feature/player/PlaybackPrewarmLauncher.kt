package dev.typetype.android.feature.player

import dev.typetype.android.core.ui.prewarm.PlaybackPrewarm
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Singleton
class PlaybackPrewarmLauncher @Inject constructor(
    private val preheater: PlaybackPreheater,
) : PlaybackPrewarm {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var inFlight = false

    override fun prewarm(videoUrl: String, knownLive: Boolean) {
        if (videoUrl.isBlank()) return
        synchronized(this) {
            if (inFlight) return
            inFlight = true
        }
        scope.launch {
            try {
                preheater.preheat(videoUrl, knownLive)
            } finally {
                synchronized(this@PlaybackPrewarmLauncher) { inFlight = false }
            }
        }
    }
}
