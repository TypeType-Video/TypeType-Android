package dev.typetype.android.feature.player

import dev.typetype.android.domain.stream.SabrPlaybackBinding
import dev.typetype.android.domain.stream.SabrPlaybackSession
import dev.typetype.android.domain.stream.SabrPlaybackTarget

internal data class PlayableSource(
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
