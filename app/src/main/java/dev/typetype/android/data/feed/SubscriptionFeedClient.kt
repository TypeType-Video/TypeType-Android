package dev.typetype.android.data.feed

import dev.typetype.android.core.error.CodedFailure
import dev.typetype.android.data.network.TypeTypeFeedApi
import dev.typetype.android.data.network.dto.SubscriptionFeedResponse
import dev.typetype.android.data.network.serverResponseException
import dev.typetype.android.data.network.dto.toDomainVideo
import dev.typetype.android.domain.feed.SubscriptionsPage
import kotlinx.coroutines.delay
import retrofit2.Response

internal class SubscriptionFeedClient(
    private val pause: suspend (Long) -> Unit = { delay(it) },
) {
    suspend fun load(
        api: TypeTypeFeedApi,
        cursor: String?,
        limit: Int,
        expectedGeneration: Long?,
        verifyOwner: suspend () -> Unit,
    ): SubscriptionsPage {
        require(limit in 1..100) { "Subscription feed page size is outside the server contract" }
        while (true) {
            verifyOwner()
            val response = api.subscriptionsFeed(limit = limit, cursor = cursor)
            verifyOwner()
            when (response.code()) {
                200 -> return response.readyPage(expectedGeneration)
                202 -> {
                    pause(response.preparationDelay())
                    verifyOwner()
                }
                else -> throw serverResponseException(response)
            }
        }
    }
}

private fun Response<SubscriptionFeedResponse>.readyPage(
    expectedGeneration: Long?,
): SubscriptionsPage {
    val body = body() ?: throw contractFailure("Subscription feed returned an empty response")
    val generation = body.generation
        ?.takeIf { it > 0L }
        ?: throw contractFailure("Subscription feed omitted a valid generation")
    val generatedAt = body.generatedAt
        ?.takeIf { it > 0L }
        ?: throw contractFailure("Subscription feed omitted its generation timestamp")
    val refreshing = body.refreshing
        ?: throw contractFailure("Subscription feed omitted its refresh state")
    if (expectedGeneration != null && generation != expectedGeneration) {
        throw SubscriptionFeedContractException(
            message = "Subscription feed changed generation during pagination",
            failureCode = GENERATION_MISMATCH_CODE,
        )
    }
    return SubscriptionsPage(
        videos = body.videos.map { it.toDomainVideo() },
        nextCursor = body.nextpage?.takeIf { it.isNotBlank() },
        generation = generation,
        generatedAtMillis = generatedAt,
        refreshing = refreshing,
    )
}

private fun Response<SubscriptionFeedResponse>.preparationDelay(): Long {
    val body = body() ?: throw contractFailure("Subscription feed preparation returned no state")
    if (body.code != PREPARING_CODE) {
        throw contractFailure("Subscription feed preparation returned an unknown state")
    }
    return body.retryAfterMs
        ?.coerceIn(MIN_RETRY_DELAY_MS, MAX_RETRY_DELAY_MS)
        ?: throw contractFailure("Subscription feed preparation omitted its retry delay")
}

private fun contractFailure(message: String): SubscriptionFeedContractException =
    SubscriptionFeedContractException(message, CONTRACT_MISMATCH_CODE)

internal class SubscriptionFeedContractException(
    message: String,
    override val failureCode: String,
) : IllegalStateException(message), CodedFailure {
    override val requestId: String? = null
    override val statusCode: Int? = null
}

internal const val INVALID_CURSOR_CODE = "subscription_feed_invalid_cursor"
internal const val STALE_GENERATION_CODE = "subscription_feed_stale_generation"
internal const val GENERATION_MISMATCH_CODE = "subscription_feed_generation_mismatch"
private const val CONTRACT_MISMATCH_CODE = "subscription_feed_contract_mismatch"
private const val PREPARING_CODE = "subscription_feed_preparing"
private const val MIN_RETRY_DELAY_MS = 100L
private const val MAX_RETRY_DELAY_MS = 5_000L
