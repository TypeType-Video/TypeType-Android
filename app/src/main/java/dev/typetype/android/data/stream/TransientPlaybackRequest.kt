package dev.typetype.android.data.stream

import dev.typetype.android.data.network.AlwaysAvailablePlaybackNetworkObserver
import dev.typetype.android.data.network.PlaybackNetworkObserver
import dev.typetype.android.data.network.isTransientHttpStatus
import dev.typetype.android.data.network.retryAfterMillis
import dev.typetype.android.data.network.transientHttpRetryDelayMs
import java.io.IOException
import retrofit2.Response

internal suspend fun <T> transientPlaybackRequest(
    pause: suspend (Long) -> Unit,
    network: PlaybackNetworkObserver = AlwaysAvailablePlaybackNetworkObserver,
    request: suspend () -> Response<T>,
): Response<T> {
    var httpErrorCount = 0
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
        val retryDelayMs = transientHttpRetryDelayMs(
            errorCount = httpErrorCount,
            maximumRetries = MAX_SERVER_RESPONSE_RETRIES,
            requestedDelayMs = response.headers().toMultimap().retryAfterMillis(),
        ) ?: return response
        response.errorBody()?.close()
        pause(retryDelayMs)
    }
}

private const val MAX_SERVER_RESPONSE_RETRIES = 24
private const val MAX_CONNECTED_TRANSPORT_RETRIES = 8
private const val MAX_OFFLINE_WAIT_MS = 30 * 60 * 1_000L
