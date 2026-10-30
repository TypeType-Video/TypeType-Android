package dev.typetype.android.core.ui.prewarm

import androidx.compose.runtime.staticCompositionLocalOf

data class PrewarmRequest(val videoUrl: String, val knownLive: Boolean)

interface PlaybackPrewarm {
    fun prefetchStreams(requests: List<PrewarmRequest>)

    fun cancelStreamPrefetch()

    fun pressSession(request: PrewarmRequest): PressedSession
}

interface PressedSession {
    fun confirm()

    fun cancel()
}

val LocalPlaybackPrewarm = staticCompositionLocalOf<PlaybackPrewarm?> { null }

val LocalSettledCardPrefetch = staticCompositionLocalOf<SettledCardPrefetch?> { null }
