package dev.typetype.android.services

import dev.typetype.android.domain.preferences.AppPreferences
import java.io.Closeable
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

internal enum class PlaybackTaskRemovalAction {
    KeepPlaying,
    PauseAndStop,
    Stop,
}

internal class PlaybackTaskRemovalPolicy(
    preferences: Flow<AppPreferences>,
    coroutineContext: CoroutineContext = Dispatchers.Main.immediate,
) : Closeable {
    private val scope = CoroutineScope(SupervisorJob() + coroutineContext)

    @Volatile
    private var pauseInBackground = false

    init {
        scope.launch {
            preferences
                .map { it.playerPauseInBackground }
                .distinctUntilChanged()
                .collect { pauseInBackground = it }
        }
    }

    fun action(playWhenReady: Boolean, mediaItemCount: Int): PlaybackTaskRemovalAction = when {
        !playWhenReady || mediaItemCount == 0 -> PlaybackTaskRemovalAction.Stop
        pauseInBackground -> PlaybackTaskRemovalAction.PauseAndStop
        else -> PlaybackTaskRemovalAction.KeepPlaying
    }

    override fun close() {
        scope.cancel()
    }
}
