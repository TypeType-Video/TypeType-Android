package dev.typetype.android.data.progress

import dev.typetype.android.data.account.AccountScope
import dev.typetype.android.data.account.ActiveAccountScope
import dev.typetype.android.data.network.TypeTypeApiHolder
import dev.typetype.android.data.network.dto.ProgressBatchRequest
import dev.typetype.android.domain.navigation.canonicalVideoIdentity
import dev.typetype.android.domain.progress.WatchProgressSource
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

// Watch positions for the video cards on screen, fetched like the web client: one batch request per list instead
// of one request per card, and kept for a minute so scrolling back and forth does not refetch them.
@Singleton
class WatchProgressCache @Inject constructor(
    private val apiHolder: TypeTypeApiHolder,
    private val activeAccountScope: ActiveAccountScope,
) : WatchProgressSource {
    private val _positions = MutableStateFlow<Map<String, Long>>(emptyMap())
    override val positions: StateFlow<Map<String, Long>> = _positions.asStateFlow()
    private val fetchedAt = HashMap<String, Long>()
    private var owner: AccountScope? = null
    private val mutex = Mutex()

    override suspend fun load(videoUrls: Collection<String>) = load(videoUrls, System.currentTimeMillis())

    suspend fun load(videoUrls: Collection<String>, now: Long) {
        val scope = runCatching { activeAccountScope.require() }.getOrNull() ?: return
        val due = mutex.withLock {
            if (owner != scope) {
                owner = scope
                fetchedAt.clear()
                _positions.value = emptyMap()
            }
            videoUrls.map(::canonicalVideoIdentity)
                .distinct()
                .filter { url -> fetchedAt[url]?.let { now - it > FRESH_FOR_MS } ?: true }
                .onEach { fetchedAt[it] = now }
        }
        due.chunked(MAX_BATCH_SIZE).forEach { batch -> fetch(scope, batch) }
    }

    private suspend fun fetch(scope: AccountScope, batch: List<String>) {
        val items = try {
            val response = withContext(Dispatchers.IO) {
                apiHolder.require(scope).progressBatch(ProgressBatchRequest(batch))
            }
            if (!response.isSuccessful) return
            response.body().orEmpty()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            mutex.withLock { batch.forEach(fetchedAt::remove) }
            return
        }
        mutex.withLock {
            if (owner != scope) return
            val found = items.associate { canonicalVideoIdentity(it.videoUrl) to it.position }
            _positions.value = _positions.value - batch.toSet() + found.filterValues { it > 0 }
        }
    }

    private companion object {
        const val MAX_BATCH_SIZE = 200
        const val FRESH_FOR_MS = 60_000L
    }
}
