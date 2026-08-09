package dev.typetype.android.feature.subscriptions

import dev.typetype.android.core.error.CodedFailure
import dev.typetype.android.data.feed.GENERATION_MISMATCH_CODE
import dev.typetype.android.data.feed.INVALID_CURSOR_CODE
import dev.typetype.android.data.feed.STALE_GENERATION_CODE
import dev.typetype.android.domain.feed.HomeFeedRepository
import dev.typetype.android.domain.feed.Video
import java.io.IOException
import kotlinx.coroutines.CancellationException

internal const val SUBSCRIPTIONS_PAGE_SIZE = 12
internal const val SERVER_REFRESH_POLL_MS = 1_000L

internal enum class SubscriptionsRecoveryRequest { Refresh, Pagination }

internal class SubscriptionsRecovery {
    private var pending: SubscriptionsRecoveryRequest? = null

    fun schedule(failure: Throwable, request: SubscriptionsRecoveryRequest) {
        if (failure.canRecoverAfterNetworkChange()) pending = request
    }

    fun clear() {
        pending = null
    }

    fun takeWhenAvailable(isAvailable: Boolean): SubscriptionsRecoveryRequest? {
        if (!isAvailable) return null
        return pending.also { pending = null }
    }
}

internal fun Throwable.requiresSubscriptionsPaginationRestart(): Boolean =
    (this as? CodedFailure)?.failureCode in setOf(
        INVALID_CURSOR_CODE,
        STALE_GENERATION_CODE,
        GENERATION_MISMATCH_CODE,
    )

private fun Throwable.canRecoverAfterNetworkChange(): Boolean {
    if (this is IOException) return true
    val statusCode = (this as? CodedFailure)?.statusCode ?: return false
    return statusCode in 500..599
}

internal suspend fun HomeFeedRepository.loadCachedSubscriptionsFeedOrEmpty(): List<Video> = try {
    loadCachedSubscriptionsFeed()
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (_: Throwable) {
    emptyList()
}
