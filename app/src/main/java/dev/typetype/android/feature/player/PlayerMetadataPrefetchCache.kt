package dev.typetype.android.feature.player

import dev.typetype.android.domain.stream.Stream
import java.util.concurrent.atomic.AtomicReference

internal class PlayerMetadataPrefetchCache {
    private val value = AtomicReference<Entry?>()

    fun put(url: String, stream: Stream) {
        value.set(Entry(url, stream))
    }

    fun take(url: String): Stream? {
        while (true) {
            val candidate = value.get() ?: return null
            if (candidate.url != url) return null
            if (value.compareAndSet(candidate, null)) return candidate.stream
        }
    }

    private data class Entry(
        val url: String,
        val stream: Stream,
    )
}
