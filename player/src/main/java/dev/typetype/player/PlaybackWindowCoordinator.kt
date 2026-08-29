package dev.typetype.player

import android.os.Handler
import java.io.IOException

internal class PlaybackWindowCoordinator(
    private val loader: PlaybackWindowLoader,
    private val tasks: PlaybackTaskDispatcher,
    private val seekSettleDelayMs: Long = SEEK_SETTLE_DELAY_MS,
) {
    constructor(
        loader: PlaybackWindowLoader,
        playbackHandler: Handler,
    ) : this(loader, HandlerPlaybackTaskDispatcher(playbackHandler))

    private var activeLoad: PlaybackLoadCancellation? = null
    private var deferredSeek: Runnable? = null
    private val listeners = linkedSetOf<Listener>()
    private var released = false
    private var failure: IOException? = null
    private var operationId = 0L

    @Volatile
    private var request = PlaybackWindowRequest(0L, emptyList())

    var window: PlaybackWindow? = null
        private set

    var isSeeking: Boolean = false
        private set

    fun addListener(listener: Listener) {
        listeners += listener
    }

    fun removeListener(listener: Listener) {
        listeners -= listener
    }

    fun seed(window: PlaybackWindow) {
        check(this.window == null)
        this.window = window
    }

    fun load(positionUs: Long, bufferedRanges: List<PlaybackBufferedRange> = emptyList()) {
        request = PlaybackWindowRequest(positionUs, bufferedRanges.toList())
        if (released || isSeeking || activeLoad != null || failure != null) return
        startOperation {
            loader.load(::currentRequest, it)
        }
    }

    fun seek(positionUs: Long) {
        if (released) return
        val shouldSettle = isSeeking || deferredSeek != null
        operationId++
        activeLoad?.cancel()
        activeLoad = null
        deferredSeek?.let(tasks::remove)
        deferredSeek = null
        isSeeking = true
        failure = null
        if (shouldSettle) {
            val task = Runnable {
                deferredSeek = null
                if (!released) startSeek(positionUs)
            }
            deferredSeek = task
            tasks.postDelayed(task, seekSettleDelayMs)
        } else {
            startSeek(positionUs)
        }
    }

    fun maybeThrowError() {
        failure?.let { throw it }
    }

    fun release() {
        released = true
        operationId++
        activeLoad?.cancel()
        activeLoad = null
        deferredSeek?.let(tasks::remove)
        deferredSeek = null
        isSeeking = false
        listeners.clear()
        loader.release()
    }

    private fun accept(result: Result<PlaybackWindow>) {
        activeLoad = null
        isSeeking = false
        result.fold(
            onSuccess = {
                window = it
                failure = null
                listeners.toList().forEach { listener -> listener.onWindowAvailable(it) }
            },
            onFailure = {
                failure = it as? IOException ?: IOException("Playback window failed", it)
                listeners.toList().forEach(Listener::onWindowFailure)
            },
        )
    }

    private fun startOperation(
        operation: ((Result<PlaybackWindow>) -> Unit) -> PlaybackLoadCancellation,
    ) {
        val requestedId = ++operationId
        activeLoad = operation { result ->
            tasks.post {
                if (!released && requestedId == operationId) accept(result)
            }
        }
    }

    private fun startSeek(positionUs: Long) {
        startOperation { loader.seek(positionUs, it) }
    }

    private fun currentRequest(): PlaybackWindowRequest = request

    interface Listener {
        fun onWindowAvailable(window: PlaybackWindow)

        fun onWindowFailure()
    }
}

internal interface PlaybackTaskDispatcher {
    fun post(task: Runnable)

    fun postDelayed(task: Runnable, delayMs: Long)

    fun remove(task: Runnable)
}

private class HandlerPlaybackTaskDispatcher(
    private val handler: Handler,
) : PlaybackTaskDispatcher {
    override fun post(task: Runnable) {
        handler.post(task)
    }

    override fun postDelayed(task: Runnable, delayMs: Long) {
        handler.postDelayed(task, delayMs)
    }

    override fun remove(task: Runnable) {
        handler.removeCallbacks(task)
    }
}

private const val SEEK_SETTLE_DELAY_MS = 80L
