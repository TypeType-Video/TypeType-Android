package dev.typetype.android.feature.player

import dev.typetype.android.domain.stream.SabrPlaybackRepository
import dev.typetype.android.domain.stream.SabrPlaybackSelection
import dev.typetype.android.domain.stream.SabrPlaybackSession
import dev.typetype.android.domain.stream.Stream
import dev.typetype.android.domain.stream.binding
import dev.typetype.android.domain.stream.sabrPlaybackTarget

internal class PlayerSabrPlayback(
    private val repository: SabrPlaybackRepository,
    private val preloads: SabrPlaybackPreloadStore = SabrPlaybackPreloadStore(),
    private val onPrepared: (stream: Stream, session: SabrPlaybackSession) -> Unit = { _, _ -> },
    private val onFailure: (stream: Stream, failure: Throwable) -> Unit,
) {
    suspend fun prepare(
        stream: Stream,
        selection: SabrPlaybackSelection,
        startTimeMs: Long,
    ): SabrPlaybackSession? {
        val target = stream.sabrPlaybackTarget(selection)
        preloads.take(target)?.await()?.let { preloaded ->
            val positioned = preloaded.resolveFor(stream)?.let { session ->
                if (startTimeMs > 0L) {
                    repository.seek(target, session.binding, startTimeMs).resolveFor(stream)
                } else {
                    session
                }
            }
            return positioned?.also { onPrepared(stream, it) }
        }
        val session = repository.prepare(target, startTimeMs).resolveFor(stream) ?: return null
        onPrepared(stream, session)
        return session
    }

    private fun Result<SabrPlaybackSession>.resolveFor(stream: Stream) = fold(
        onSuccess = { it },
        onFailure = {
            onFailure(stream, it)
            null
        },
    )
}
