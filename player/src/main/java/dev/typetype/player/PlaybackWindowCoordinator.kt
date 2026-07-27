package dev.typetype.player

import android.os.Handler
import java.io.IOException

internal class PlaybackWindowCoordinator(
    private val loader: PlaybackWindowLoader,
    private val playbackHandler: Handler,
) {
    private var activeLoad: PlaybackLoadCancellation? = null
    private var listener: Listener? = null
    private var released = false
    private var failure: IOException? = null
    private var operationId = 0L

    @Volatile
    private var request = PlaybackWindowRequest(0L, emptyList())

    var window: PlaybackWindow? = null
        private set

    var isSeeking: Boolean = false
        private set

    fun setListener(listener: Listener?) {
        this.listener = listener
    }

    fun seed(window: PlaybackWindow) {
        check(this.window == null)
        this.window = window
    }

    fun load(positionUs: Long, bufferedRanges: List<PlaybackBufferedRange> = emptyList()) {
        request = PlaybackWindowRequest(positionUs, bufferedRanges.toList())
        if (released || activeLoad != null || failure != null) return
        startOperation {
            loader.load(::currentRequest, it)
        }
    }

    fun seek(positionUs: Long) {
        if (released) return
        activeLoad?.cancel()
        activeLoad = null
        isSeeking = true
        failure = null
        startOperation { loader.seek(positionUs, it) }
    }

    fun maybeThrowError() {
        failure?.let { throw it }
    }

    fun release() {
        released = true
        operationId++
        activeLoad?.cancel()
        activeLoad = null
        isSeeking = false
        listener = null
        loader.release()
    }

    private fun accept(result: Result<PlaybackWindow>) {
        activeLoad = null
        isSeeking = false
        result.fold(
            onSuccess = {
                window = it
                failure = null
                listener?.onWindowAvailable(it)
            },
            onFailure = {
                failure = it as? IOException ?: IOException("Playback window failed", it)
                listener?.onWindowFailure()
            },
        )
    }

    private fun startOperation(
        operation: ((Result<PlaybackWindow>) -> Unit) -> PlaybackLoadCancellation,
    ) {
        val requestedId = ++operationId
        activeLoad = operation { result ->
            playbackHandler.post {
                if (!released && requestedId == operationId) accept(result)
            }
        }
    }

    private fun currentRequest(): PlaybackWindowRequest = request

    interface Listener {
        fun onWindowAvailable(window: PlaybackWindow)

        fun onWindowFailure()
    }
}
