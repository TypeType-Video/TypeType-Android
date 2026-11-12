package dev.typetype.android.feature.player

import dev.typetype.android.domain.stream.SabrPlaybackRepository
import dev.typetype.android.domain.stream.Stream
import dev.typetype.android.services.SabrPlaybackWindowCache
import javax.inject.Inject
import dev.typetype.android.domain.preferences.PreferencesRepository
import kotlinx.coroutines.flow.first

class PlayerSabrPlaybackFactory @Inject constructor(
    private val repository: SabrPlaybackRepository,
    private val preloads: SabrPlaybackPreloadStore,
    private val windowCache: SabrPlaybackWindowCache,
    private val preferences: PreferencesRepository,
) {
    internal fun create(
        onFailure: (stream: Stream, failure: Throwable) -> Unit,
    ) = PlayerSabrPlayback(
        repository = repository,
        preloads = preloads,
        onPrepared = { _, session ->
            windowCache.put(session)
        },
        onFailure = onFailure,
        bufferSeconds = { preferences.observe().first().playerYouTubeBufferSeconds },
    )
}
