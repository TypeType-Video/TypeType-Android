package dev.typetype.android.data.stream

import dev.typetype.android.data.network.AlwaysAvailablePlaybackNetworkObserver
import dev.typetype.android.data.network.PlaybackNetworkObserver
import dev.typetype.android.data.network.isTransientHttpStatus
import dev.typetype.android.data.network.retryAfterMillis
import dev.typetype.android.data.network.transientHttpRetryDelayMs
import java.io.IOException
import retrofit2.Response

internal enum class TransientServerWait(val maxRetries: Int, val maxRequestedDelayMs: Long) {
    Playback(maxRetries = 6, maxRequestedDelayMs = Long.MAX_VALUE),

    // Opening a video follows the web client: two retries at most, and a server asking to wait longer than a
    // page should (a provider block answers Retry-After: 60) fails at once so its message shows right away.
    Discovery(maxRetries = 2, maxRequestedDelayMs = 15_000L),
}

internal suspend fun <T> transientPlaybackRequest(
    pause: suspend (Long) -> Unit,
    network: PlaybackNetworkObserver = AlwaysAvailablePlaybackNetworkObserver,
    serverWait: TransientServerWait = TransientServerWait.Playback,
    request: suspend () -> Response<T>,
): Response<T> {
    var httpErrorCount = 0
    var serverWaitMs = 0L
    var transportErrorCount = 0
    var transportGeneration = network.snapshot().generation
    while (true) {
        val response = try {
            request()
        } catch (failure: IOException) {
            val state = network.snapshot()
            if (state.generation != transportGeneration) {
                transportGeneration = state.generation
                transportErrorCount = 0
            }
            transportErrorCount++
            if (!state.isAvailable) {
                val restored = network.awaitAvailableAfter(
                    generation = state.generation,
                    timeoutMs = MAX_OFFLINE_WAIT_MS,
                )
                if (!restored) throw failure
                transportGeneration = network.snapshot().generation
                transportErrorCount = 0
            } else {
                val retryDelayMs = transientHttpRetryDelayMs(
                    errorCount = transportErrorCount,
                    maximumRetries = MAX_CONNECTED_TRANSPORT_RETRIES,
                    requestedDelayMs = null,
                ) ?: throw failure
                pause(retryDelayMs)
            }
            continue
        }
        if (!isTransientHttpStatus(response.code())) return response
        httpErrorCount++
        val requestedDelayMs = response.headers().toMultimap().retryAfterMillis()
        if (requestedDelayMs != null && requestedDelayMs > serverWait.maxRequestedDelayMs) return response
        val retryDelayMs = transientHttpRetryDelayMs(
            errorCount = httpErrorCount,
            maximumRetries = serverWait.maxRetries,
            requestedDelayMs = requestedDelayMs,
        ) ?: return response
        if (serverWaitMs + retryDelayMs > MAX_SERVER_WAIT_MS) return response
        serverWaitMs += retryDelayMs
        response.errorBody()?.close()
        pause(retryDelayMs)
    }
}

private const val MAX_CONNECTED_TRANSPORT_RETRIES = 8
private const val MAX_OFFLINE_WAIT_MS = 10_000L
private const val MAX_SERVER_WAIT_MS = 60_000L
