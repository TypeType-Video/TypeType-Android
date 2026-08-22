package dev.typetype.android.feature.player

import dev.typetype.android.domain.stream.SabrPlaybackSession
import dev.typetype.android.domain.stream.SabrPlaybackTarget
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred

@Singleton
class SabrPlaybackPreloadStore(
    private val nanoTime: () -> Long,
) {
    @Inject
    constructor() : this(System::nanoTime)

    private val lock = Any()
    private val entries = linkedMapOf<SabrPlaybackTarget, Entry>()

    fun reserve(target: SabrPlaybackTarget): Reservation {
        val now = nanoTime()
        synchronized(lock) {
            removeExpired(now)
            entries[target]?.let {
                return Reservation(it.result, owner = false)
            }
            val created = Entry(
                expiresAtNanos = now + PRELOAD_TTL_NANOS,
                result = CompletableDeferred(),
            )
            if (entries.size == MAX_PRELOADS) entries.remove(entries.keys.first())
            entries[target] = created
            return Reservation(created.result, owner = true)
        }
    }

    fun take(target: SabrPlaybackTarget): Deferred<Result<SabrPlaybackSession>>? {
        val now = nanoTime()
        return synchronized(lock) {
            removeExpired(now)
            entries.remove(target)?.result
        }
    }

    private fun removeExpired(now: Long) {
        entries.entries.removeAll { it.value.expiresAtNanos <= now }
    }

    data class Reservation(
        val result: CompletableDeferred<Result<SabrPlaybackSession>>,
        val owner: Boolean,
    )

    private data class Entry(
        val expiresAtNanos: Long,
        val result: CompletableDeferred<Result<SabrPlaybackSession>>,
    )
}

private const val PRELOAD_TTL_NANOS = 120_000_000_000L
private const val MAX_PRELOADS = 2
