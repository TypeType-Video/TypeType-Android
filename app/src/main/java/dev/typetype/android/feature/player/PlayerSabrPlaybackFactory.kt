package dev.typetype.android.feature.player

import dev.typetype.android.domain.stream.SabrPlaybackRepository
import dev.typetype.android.domain.stream.Stream
import dev.typetype.android.services.SabrPlaybackWindowCache
import javax.inject.Inject

class PlayerSabrPlaybackFactory @Inject constructor(
    private val repository: SabrPlaybackRepository,
    private val windowCache: SabrPlaybackWindowCache,
) {
    internal fun create(
        onFailure: (stream: Stream, failure: Throwable) -> Unit,
    ) = PlayerSabrPlayback(
        repository = repository,
        onPrepared = { _, session ->
            windowCache.put(session)
        },
        onFailure = onFailure,
    )
}
