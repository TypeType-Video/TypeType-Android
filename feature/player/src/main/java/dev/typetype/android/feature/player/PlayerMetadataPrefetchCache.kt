package dev.typetype.android.feature.player

import dev.typetype.android.domain.stream.Stream

internal class PlayerMetadataPrefetchCache {
    private val entries = LinkedHashMap<String, Stream>(MAX_ENTRIES, 0.75f, true)

    @Synchronized
    fun put(url: String, stream: Stream) {
        entries[url] = stream
        while (entries.size > MAX_ENTRIES) {
            entries.remove(entries.keys.first())
        }
    }

    @Synchronized
    fun get(url: String): Stream? = entries[url]

    private companion object {
        const val MAX_ENTRIES = 3
    }
}
