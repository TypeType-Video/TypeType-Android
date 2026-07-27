package dev.typetype.android.services

import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import dev.typetype.android.core.error.CodedFailure
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
        return failure.sabrRetryDelayMs() ?: delegate.getRetryDelayMsFor(loadErrorInfo)
    }

    override fun getMinimumLoadableRetryCount(dataType: Int): Int =
        maxOf(delegate.getMinimumLoadableRetryCount(dataType), SABR_MIN_LOADABLE_RETRY_COUNT)

    override fun onLoadTaskConcluded(loadTaskId: Long) {
        delegate.onLoadTaskConcluded(loadTaskId)
    }
}

@UnstableApi
internal fun IOException.sabrRetryDelayMs(): Long? {
    var current: Throwable? = this
    repeat(MAX_CAUSE_DEPTH) {
        val response = current as? HttpDataSource.InvalidResponseCodeException
        if (
            response?.responseCode == SABR_RETRY_RESPONSE_CODE &&
            response.dataSpec.uri.pathSegments.isSabrPlaybackPayloadPath()
        ) {
            return response.responseBody.retryAfterMs()
        }
        current = current?.cause?.takeUnless { it === current }
    }
    return null
}

private fun Throwable.hasFailureCode(code: String): Boolean {
    var current: Throwable? = this
    repeat(MAX_CAUSE_DEPTH) {
        if ((current as? CodedFailure)?.failureCode == code) return true
        current = current?.cause?.takeUnless { it === current }
    }
    return false
}

private fun ByteArray.retryAfterMs(): Long {
    val body = toString(Charsets.UTF_8)
    val requested = RETRY_AFTER_PATTERN.find(body)?.groupValues?.get(1)?.toLongOrNull()
    return (requested ?: DEFAULT_SABR_RETRY_MS).coerceIn(MIN_SABR_RETRY_MS, MAX_SABR_RETRY_MS)
}

private val RETRY_AFTER_PATTERN = Regex("\\\"retryAfterMs\\\"\\s*:\\s*(\\d+)")
private const val SABR_RETRY_RESPONSE_CODE = 202
private const val DEFAULT_SABR_RETRY_MS = 250L
private const val MIN_SABR_RETRY_MS = 100L
private const val MAX_SABR_RETRY_MS = 1_000L
private const val MAX_CAUSE_DEPTH = 8
private const val SABR_MIN_LOADABLE_RETRY_COUNT = 120
private const val SABR_CONTRACT_FAILURE_CODE = "youtube_sabr_contract_mismatch"
