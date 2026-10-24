package dev.typetype.android.playback

import dev.typetype.android.domain.stream.SabrPlaybackBinding
import dev.typetype.android.domain.stream.SabrPlaybackSession
import dev.typetype.android.domain.stream.SabrPlaybackTarget

data class PlayableSource(
    val url: String,
    val mimeType: String?,
    val audioUrl: String? = null,
    val audioMimeType: String? = null,
    val sourceKey: String = url,
    val sabrRequestKey: String? = null,
    val sabrBinding: SabrPlaybackBinding? = null,
    val sabrTarget: SabrPlaybackTarget? = null,
    val sabrSession: SabrPlaybackSession? = null,
)

fun samePlayableSource(
    currentUrl: String?,
    currentSourceKey: String?,
    currentAudioUrl: String?,
    currentSabrBinding: SabrPlaybackBinding? = null,
    currentSabrTarget: SabrPlaybackTarget? = null,
    requestedSource: PlayableSource,
): Boolean = currentUrl == requestedSource.url &&
    currentSourceKey == requestedSource.sourceKey &&
    currentAudioUrl == requestedSource.audioUrl &&
    currentSabrBinding == requestedSource.sabrBinding &&
    currentSabrTarget == requestedSource.sabrTarget
