package dev.typetype.android.data.feed

import dev.typetype.android.data.network.TypeTypeApi
import dev.typetype.android.data.network.dto.toDomainVideo
import dev.typetype.android.data.network.requireSuccessfulResponse
import dev.typetype.android.domain.feed.ShortsContinuation
import dev.typetype.android.domain.feed.ShortsPage
import dev.typetype.android.domain.feed.Video
import dev.typetype.android.domain.feed.shortIdentity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal suspend fun loadRecommendationShorts(
    api: TypeTypeApi,
    cursor: String?,
    service: Int,
    limit: Int,
): ShortsPage {
    val response = withContext(Dispatchers.IO) {
        api.shortsRecommendations(service, limit, "auto", cursor)
    }
    response.requireSuccessfulResponse()
    val body = response.body() ?: error("Empty Shorts recommendations body")
    return ShortsPage(
        videos = body.items.map { it.toDomainVideo() },
        continuation = body.nextCursor
            ?.takeIf { body.hasMore && it.isNotBlank() }
            ?.let(ShortsContinuation::Recommendations),
    )
}

internal suspend fun loadSubscriptionShorts(
    api: TypeTypeApi,
    page: Int,
    service: Int,
    limit: Int,
): ShortsPage {
    val response = withContext(Dispatchers.IO) {
        api.subscriptionShorts(page, limit, service, blended = true)
    }
    response.requireSuccessfulResponse()
    val body = response.body() ?: error("Empty subscription Shorts body")
    return ShortsPage(
        videos = body.videos.map { it.toDomainVideo() },
        continuation = body.nextpage
            ?.toIntOrNull()
            ?.takeIf { it > page }
            ?.let(ShortsContinuation::Subscriptions),
    )
}

internal suspend fun loadDiscoveryShorts(
    api: TypeTypeApi,
    nextPage: String?,
    service: Int,
): ShortsPage {
    val response = withContext(Dispatchers.IO) {
        api.search(query = "shorts", service = service, nextpage = nextPage)
    }
    response.requireSuccessfulResponse()
    val body = response.body() ?: error("Empty Shorts discovery body")
    return ShortsPage(
        videos = body.items.map { it.toDomainVideo() },
        continuation = body.nextpage
            ?.takeIf(String::isNotBlank)
            ?.let(ShortsContinuation::Discovery),
    )
}

internal fun List<Video>.normalizedShorts(): List<Video> = asSequence()
    .filter(Video::isLikelyShort)
    .distinctBy(Video::shortIdentity)
    .groupBy { it.uploaderUrl.ifBlank { "__${it.id}" } }
    .values
    .map { channel -> ArrayDeque(channel) }
    .let { channels ->
        buildList {
            while (channels.any { it.isNotEmpty() }) {
                channels.forEach { channel -> channel.removeFirstOrNull()?.let(::add) }
            }
        }
    }

internal fun ShortsPage.normalized(limit: Int): ShortsPage = copy(
    videos = videos.normalizedShorts().take(limit),
)

private fun Video.isLikelyShort(): Boolean =
    isShortFormContent || url.contains("/shorts/") || durationSeconds in 0L..180L
