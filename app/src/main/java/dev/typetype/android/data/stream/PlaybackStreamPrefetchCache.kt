package dev.typetype.android.data.stream

import dev.typetype.android.domain.stream.Stream
import dev.typetype.android.domain.stream.StreamRequestScope

internal class PlaybackStreamPrefetchCache(
    private val nowMillis: () -> Long = System::currentTimeMillis,
    private val ttlMillis: Long = DEFAULT_TTL_MILLIS,
) {
    private val entries = LinkedHashMap<Key, Entry>(MAX_ENTRIES, 0.75f, true)

    @Synchronized
    fun put(videoUrl: String, stream: Stream) {
        val scope = stream.requestScope ?: return
        removeExpired()
        entries[Key(videoUrl, scope)] = Entry(stream, nowMillis() + ttlMillis)
        while (entries.size > MAX_ENTRIES) {
            entries.remove(entries.keys.first())
        }
    }

    @Synchronized
    fun get(videoUrl: String, scope: StreamRequestScope): Stream? {
        removeExpired()
        return entries[Key(videoUrl, scope)]?.stream
    }

    private fun removeExpired() {
        val now = nowMillis()
        entries.entries.removeAll { it.value.expiresAtMillis <= now }
    }

    private data class Key(
        val videoUrl: String,
        val scope: StreamRequestScope,
    )

    private data class Entry(
        val stream: Stream,
        val expiresAtMillis: Long,
    )

    private companion object {
        const val MAX_ENTRIES = 3
        const val DEFAULT_TTL_MILLIS = 120_000L
    }
}
