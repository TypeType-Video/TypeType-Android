package dev.typetype.android.data.feed

import dev.typetype.android.core.error.CodedFailure
import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Test

class SubscriptionFeedDiagnosticsTest {
    @Test
    fun classifiesContractDecodePaginationAndServerFailures() {
        assertEquals(
            "/subscriptions/feed/contract",
            subscriptionFeedDiagnosticRoute(
                SubscriptionFeedContractException("contract", "contract"),
                isPagination = false,
            ),
        )
        assertEquals(
            "/subscriptions/feed/decode",
            subscriptionFeedDiagnosticRoute(
                SerializationException("decode"),
                isPagination = false,
            ),
        )
        assertEquals(
            "/subscriptions/feed/pagination",
            subscriptionFeedDiagnosticRoute(
                SubscriptionFeedContractException("cursor", INVALID_CURSOR_CODE),
                isPagination = true,
            ),
        )
        assertEquals(
            "/subscriptions/feed/server",
            subscriptionFeedDiagnosticRoute(ServerFailure(), isPagination = false),
        )
    }
}

private class ServerFailure : IllegalStateException(), CodedFailure {
    override val failureCode = "server_failure"
    override val requestId = "request-id"
    override val statusCode = 503
}
