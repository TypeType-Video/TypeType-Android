package dev.typetype.android.feature.subscriptions

import dev.typetype.android.core.error.CodedFailure
import dev.typetype.android.data.feed.STALE_GENERATION_CODE
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SubscriptionsRecoveryTest {
    @Test
    fun networkFailureRetriesRefreshOnceAfterConnectivityReturns() {
        val recovery = SubscriptionsRecovery()
        recovery.schedule(IOException("offline"), SubscriptionsRecoveryRequest.Refresh)

        assertNull(recovery.takeWhenAvailable(isAvailable = false))
        assertEquals(
            SubscriptionsRecoveryRequest.Refresh,
            recovery.takeWhenAvailable(isAvailable = true),
        )
        assertNull(recovery.takeWhenAvailable(isAvailable = true))
    }

    @Test
    fun serverFailureRetriesTheExactPaginationOperation() {
        val recovery = SubscriptionsRecovery()
        recovery.schedule(Failure(status = 530), SubscriptionsRecoveryRequest.Pagination)

        assertEquals(
            SubscriptionsRecoveryRequest.Pagination,
            recovery.takeWhenAvailable(isAvailable = true),
        )
    }

    @Test
    fun clientFailureDoesNotCreateAnAutomaticRetryLoop() {
        val recovery = SubscriptionsRecovery()
        recovery.schedule(Failure(status = 400), SubscriptionsRecoveryRequest.Refresh)

        assertNull(recovery.takeWhenAvailable(isAvailable = true))
    }

    @Test
    fun staleGenerationRequiresACompleteRefresh() {
        assertEquals(
            true,
            Failure(status = 409, code = STALE_GENERATION_CODE)
                .requiresSubscriptionsPaginationRestart(),
        )
    }

    private data class Failure(
        val status: Int,
        val code: String? = null,
    ) : RuntimeException(), CodedFailure {
        override val failureCode = code
        override val requestId: String? = null
        override val statusCode = status
    }
}
