package dev.typetype.android.data.feed

import dev.typetype.android.core.error.CodedFailure
import dev.typetype.android.data.diagnostics.LocalDiagnosticsRepository
import javax.inject.Inject
import kotlinx.serialization.SerializationException

class SubscriptionFeedDiagnostics @Inject constructor(
    private val repository: LocalDiagnosticsRepository,
) {
    fun recordReady() {
        repository.recordLocalEvent(READY_ROUTE)
    }

    fun recordFailure(failure: Throwable, isPagination: Boolean) {
        val coded = failure as? CodedFailure
        repository.recordLocalEvent(
            route = subscriptionFeedDiagnosticRoute(failure, isPagination),
            statusCode = coded?.statusCode,
            requestId = coded?.requestId,
        )
    }

    fun recordPersistenceFailure() {
        repository.recordLocalEvent(PERSISTENCE_ROUTE)
    }
}

internal fun subscriptionFeedDiagnosticRoute(
    failure: Throwable,
    isPagination: Boolean,
): String = when {
    failure is SerializationException -> DECODE_ROUTE
    failure is SubscriptionFeedContractException &&
        (isPagination || failure.failureCode == GENERATION_MISMATCH_CODE) -> PAGINATION_ROUTE
    failure is SubscriptionFeedContractException -> CONTRACT_ROUTE
    failure is CodedFailure -> SERVER_ROUTE
    isPagination -> PAGINATION_ROUTE
    else -> DECODE_ROUTE
}

private const val READY_ROUTE = "/subscriptions/feed/ready"
private const val CONTRACT_ROUTE = "/subscriptions/feed/contract"
private const val DECODE_ROUTE = "/subscriptions/feed/decode"
private const val PAGINATION_ROUTE = "/subscriptions/feed/pagination"
private const val PERSISTENCE_ROUTE = "/subscriptions/feed/persistence"
private const val SERVER_ROUTE = "/subscriptions/feed/server"
