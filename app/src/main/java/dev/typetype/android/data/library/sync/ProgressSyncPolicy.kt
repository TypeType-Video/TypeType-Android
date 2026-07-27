package dev.typetype.android.data.library.sync

import dev.typetype.android.core.error.CodedFailure
import java.io.IOException

internal fun shouldRetryProgressSync(failure: Throwable): Boolean {
    val status = (failure as? CodedFailure)?.statusCode
    return failure is IOException || status == 408 || status == 429 || status != null && status >= 500
}

internal fun isProgressSyncTargetCurrent(
    accountGeneration: Long?,
    currentBaseUrl: String?,
    expectedGeneration: Long,
    expectedBaseUrl: String,
): Boolean {
    return expectedGeneration >= 0L &&
        accountGeneration == expectedGeneration &&
        currentBaseUrl == expectedBaseUrl
}
