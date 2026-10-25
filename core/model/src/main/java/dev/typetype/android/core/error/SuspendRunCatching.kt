package dev.typetype.android.core.error

import kotlin.coroutines.cancellation.CancellationException

// runCatching for suspending work: a cancelled coroutine stays cancelled instead of turning into a failed Result,
// so a closed screen or a replaced request does not report an error or keep running.
inline fun <T> suspendRunCatching(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (failure: Throwable) {
    Result.failure(failure)
}
