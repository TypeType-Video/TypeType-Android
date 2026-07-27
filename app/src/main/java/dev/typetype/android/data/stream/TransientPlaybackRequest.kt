package dev.typetype.android.data.stream

import dev.typetype.android.data.network.isTransientHttpStatus
import dev.typetype.android.data.network.retryAfterMillis
import dev.typetype.android.data.network.transientHttpRetryDelayMs
import java.io.IOException
import retrofit2.Response

internal suspend fun <T> transientPlaybackRequest(
    pause: suspend (Long) -> Unit,
    request: suspend () -> Response<T>,
): Response<T> {
    var errorCount = 0
    while (true) {
        val response = try {
            request()
        } catch (failure: IOException) {
            errorCount++
            val retryDelayMs = transientHttpRetryDelayMs(errorCount, null) ?: throw failure
            pause(retryDelayMs)
            continue
        }
        if (!isTransientHttpStatus(response.code())) return response
        errorCount++
        val retryDelayMs = transientHttpRetryDelayMs(
            errorCount = errorCount,
            requestedDelayMs = response.headers().toMultimap().retryAfterMillis(),
        ) ?: return response
        response.errorBody()?.close()
        pause(retryDelayMs)
    }
}
