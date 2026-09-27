package dev.typetype.android.feature.player

import androidx.compose.runtime.staticCompositionLocalOf

fun interface PlaybackPrewarm {
    fun prewarm(videoUrl: String)
}

val LocalPlaybackPrewarm = staticCompositionLocalOf<PlaybackPrewarm?> { null }

const val CARD_PREWARM_DELAY_MILLIS = 200L
