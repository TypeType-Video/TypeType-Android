package dev.typetype.android.core.ui.prewarm

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SettledCardPrefetch(
    private val scope: CoroutineScope,
    private val prewarm: PlaybackPrewarm,
    private val now: () -> Long = { System.nanoTime() / NANOS_PER_MILLI },
    private val wait: suspend (Long) -> Unit = { delay(it) },
) {
    private val cards = linkedMapOf<Any, CardPlacement>()
    private var settleJob: Job? = null
    private var prefetching = false
    private var movedAtMillis = 0L

    fun place(key: Any, request: PrewarmRequest, visibleFraction: Float, top: Float) {
        val placement = CardPlacement(request, visibleFraction, top)
        if (cards.put(key, placement) == placement) return
        onMoved()
    }

    fun remove(key: Any) {
        if (cards.remove(key) != null) onMoved()
    }

    private fun onMoved() {
        movedAtMillis = now()
        if (prefetching) {
            prewarm.cancelStreamPrefetch()
            prefetching = false
        }
        if (settleJob?.isActive == true) return
        settleJob = scope.launch {
            var quietMillis = now() - movedAtMillis
            while (quietMillis < SETTLE_DELAY_MILLIS) {
                wait(SETTLE_DELAY_MILLIS - quietMillis)
                quietMillis = now() - movedAtMillis
            }
            prefetchMostVisible()
        }
    }

    private fun prefetchMostVisible() {
        val requests = cards.values
            .filter { it.visibleFraction >= MIN_VISIBLE_FRACTION }
            .sortedWith(compareByDescending<CardPlacement> { it.visibleFraction }.thenBy { it.top })
            .take(MAX_SETTLED_CARDS)
            .map { it.request }
        if (requests.isEmpty()) return
        prefetching = true
        prewarm.prefetchStreams(requests)
    }

    private data class CardPlacement(
        val request: PrewarmRequest,
        val visibleFraction: Float,
        val top: Float,
    )

    private companion object {
        const val SETTLE_DELAY_MILLIS = 600L
        const val MIN_VISIBLE_FRACTION = 0.5f
        const val MAX_SETTLED_CARDS = 2
        const val NANOS_PER_MILLI = 1_000_000L
    }
}
