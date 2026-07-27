package dev.typetype.android.services

import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import dev.typetype.android.core.error.CodedFailure
import dev.typetype.android.data.network.isTransientHttpStatus
import dev.typetype.android.data.network.retryAfterMillis
import dev.typetype.android.data.network.transientHttpRetryDelayMs
import java.io.IOException

@UnstableApi
internal class SabrLoadErrorHandlingPolicy(
    private val delegate: LoadErrorHandlingPolicy,
) : LoadErrorHandlingPolicy {
    override fun getFallbackSelectionFor(
        fallbackOptions: LoadErrorHandlingPolicy.FallbackOptions,
        loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo,
    ): LoadErrorHandlingPolicy.FallbackSelection? =
        delegate.getFallbackSelectionFor(fallbackOptions, loadErrorInfo)

    override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo): Long {
        val failure = loadErrorInfo.exception
        if (failure.hasFailureCode(SABR_CONTRACT_FAILURE_CODE)) return C.TIME_UNSET
        val response = failure.httpResponse()
        if (response != null) {
            if (
                response.responseCode == SABR_RETRY_RESPONSE_CODE &&
                response.dataSpec.uri.pathSegments.isSabrPlaybackPayloadPath()
            ) {
                return sabrSegmentRetryDelayMs(
                    errorCount = loadErrorInfo.errorCount,
                    requestedDelayMs = response.responseBody.sabrResponseRetryAfterMs(),
                )
            }
            if (isTransientHttpStatus(response.responseCode)) {
                return transientHttpRetryDelayMs(
                    errorCount = loadErrorInfo.errorCount,
                    requestedDelayMs = response.headerFields.retryAfterMillis(),
                ) ?: C.TIME_UNSET
            }
            return C.TIME_UNSET
        }
        if (failure.hasTransientHttpTransportFailure()) {
            return transientHttpRetryDelayMs(loadErrorInfo.errorCount, null) ?: C.TIME_UNSET
        }
        return delegate.getRetryDelayMsFor(loadErrorInfo)
    }

    override fun getMinimumLoadableRetryCount(dataType: Int): Int =
        delegate.getMinimumLoadableRetryCount(dataType)

    override fun onLoadTaskConcluded(loadTaskId: Long) {
        delegate.onLoadTaskConcluded(loadTaskId)
    }
}

@UnstableApi
private fun IOException.httpResponse(): HttpDataSource.InvalidResponseCodeException? {
    var current: Throwable? = this
    repeat(MAX_CAUSE_DEPTH) {
        val response = current as? HttpDataSource.InvalidResponseCodeException
        if (response != null) return response
        current = current?.cause?.takeUnless { it === current }
    }
    return null
}

private fun IOException.hasTransientHttpTransportFailure(): Boolean {
    var current: Throwable? = this
    repeat(MAX_CAUSE_DEPTH) {
        if (
            current is HttpDataSource.HttpDataSourceException &&
            current !is HttpDataSource.InvalidContentTypeException &&
            current !is HttpDataSource.CleartextNotPermittedException
        ) {
            return true
        }
        current = current?.cause?.takeUnless { it === current }
    }
    return false
}

private fun Throwable.hasFailureCode(code: String): Boolean {
    var current: Throwable? = this
    repeat(MAX_CAUSE_DEPTH) {
        if ((current as? CodedFailure)?.failureCode == code) return true
        current = current?.cause?.takeUnless { it === current }
    }
    return false
}

internal fun sabrSegmentRetryDelayMs(errorCount: Int, requestedDelayMs: Long?): Long =
    boundedRetryDelayMs(
        errorCount = errorCount,
        maximumRetries = MAX_SEGMENT_RETRIES,
        requestedDelayMs = requestedDelayMs,
        defaultDelayMs = DEFAULT_SABR_RETRY_MS,
        minimumDelayMs = MIN_SABR_RETRY_MS,
    )

private fun boundedRetryDelayMs(
    errorCount: Int,
    maximumRetries: Int,
    requestedDelayMs: Long?,
    defaultDelayMs: Long,
    minimumDelayMs: Long,
): Long {
    if (errorCount > maximumRetries) return C.TIME_UNSET
    return (requestedDelayMs ?: defaultDelayMs)
        .coerceIn(minimumDelayMs, MAX_SEGMENT_RETRY_DELAY_MS)
}

internal fun ByteArray.sabrResponseRetryAfterMs(): Long? {
    val body = toString(Charsets.UTF_8)
    return RETRY_AFTER_PATTERN.find(body)?.groupValues?.get(1)?.toLongOrNull()
}

private val RETRY_AFTER_PATTERN = Regex("\\\"retryAfterMs\\\"\\s*:\\s*(\\d+)")
private const val SABR_RETRY_RESPONSE_CODE = 202
private const val DEFAULT_SABR_RETRY_MS = 250L
private const val MIN_SABR_RETRY_MS = 100L
private const val MAX_SEGMENT_RETRY_DELAY_MS = 3_000L
private const val MAX_CAUSE_DEPTH = 8
private const val MAX_SEGMENT_RETRIES = 59
private const val SABR_CONTRACT_FAILURE_CODE = "youtube_sabr_contract_mismatch"
