package dev.typetype.android.data.progress

internal class WatchProgressSchedule(private val freshForMs: Long, private val maxBatchSize: Int) {
    private val fetchedAt = HashMap<String, Long>()

    fun dueBatches(videoUrls: Collection<String>, now: Long): List<List<String>> =
        videoUrls.distinct()
            .filter { url -> fetchedAt[url]?.let { now - it > freshForMs } ?: true }
            .onEach { fetchedAt[it] = now }
            .chunked(maxBatchSize)

    fun forget(videoUrls: Collection<String>) {
        videoUrls.forEach(fetchedAt::remove)
    }

    fun clear() {
        fetchedAt.clear()
    }
}
