package dev.typetype.android.data.feed

import dev.typetype.android.data.account.ActiveAccountScope
import dev.typetype.android.data.network.TypeTypeApiHolder
import dev.typetype.android.data.network.dto.toDomainVideo
import dev.typetype.android.data.network.requireSuccessfulResponse
import dev.typetype.android.domain.feed.HomeFeedRepository
import dev.typetype.android.domain.feed.HomeRecommendationsPage
import dev.typetype.android.domain.feed.SubscriptionsPage
import dev.typetype.android.domain.feed.ShortsContinuation
import dev.typetype.android.domain.feed.ShortsPage
import dev.typetype.android.domain.feed.Video
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class HomeFeedRepositoryImpl @Inject constructor(
    private val apiHolder: TypeTypeApiHolder,
    private val activeAccountScope: ActiveAccountScope,
    private val feedVideoDao: FeedVideoDao,
) : HomeFeedRepository {
    private val subscriptionFeedClient = SubscriptionFeedClient()

    override suspend fun loadCachedHomeFeed(): List<Video> = loadCachedFeed(HOME_FEED_KEY)

    override suspend fun loadCachedSubscriptionsFeed(): List<Video> =
        loadCachedFeed(SUBSCRIPTIONS_FEED_KEY)

    override suspend fun cacheHomeFeed(videos: List<Video>, append: Boolean) {
        cacheFeed(HOME_FEED_KEY, videos, append)
    }

    override suspend fun cacheSubscriptionsFeed(videos: List<Video>, append: Boolean) {
        cacheFeed(SUBSCRIPTIONS_FEED_KEY, videos, append)
    }

    override suspend fun loadHomeRecommendations(
        cursor: String?,
        limit: Int,
    ): Result<HomeRecommendationsPage> = feedResult {
        val scope = activeAccountScope.require()
        val api = apiHolder.require(scope)
        val response = withContext(Dispatchers.IO) {
            api.homeRecommendations(intent = "auto", cursor = cursor, limit = limit)
        }
        response.requireSuccessfulResponse()
        val body = response.body() ?: error("Empty recommendations body")
        activeAccountScope.verify(scope)
        HomeRecommendationsPage(
            videos = body.items.map { it.toDomainVideo() },
            nextCursor = body.nextCursor?.takeIf { body.hasMore && it.isNotBlank() },
        )
    }

    override suspend fun loadTrending(): Result<List<Video>> = feedResult {
        val scope = activeAccountScope.require()
        val api = apiHolder.require(scope)
        val response = withContext(Dispatchers.IO) { api.trending() }
        response.requireSuccessfulResponse()
        val body = response.body() ?: error("Empty trending body")
        activeAccountScope.verify(scope)
        body.map { it.toDomainVideo() }
    }

    override suspend fun loadSubscriptionsFeed(
        cursor: String?,
        limit: Int,
        expectedGeneration: Long?,
    ): Result<SubscriptionsPage> = feedResult {
        val scope = activeAccountScope.require()
        val api = apiHolder.require(scope)
        subscriptionFeedClient.load(
            api = api,
            cursor = cursor,
            limit = limit,
            expectedGeneration = expectedGeneration,
            verifyOwner = { activeAccountScope.verify(scope) },
        )
    }

    override suspend fun loadShorts(
        continuation: ShortsContinuation?,
        service: Int,
        limit: Int,
    ): Result<ShortsPage> = feedResult {
        val scope = activeAccountScope.require()
        val api = apiHolder.require(scope)
        val page = when {
            scope.accountId.startsWith("guest:") -> loadDiscoveryShorts(
                api = api,
                nextPage = (continuation as? ShortsContinuation.Discovery)?.nextPage,
                service = service,
            ).normalized(limit)
            continuation is ShortsContinuation.Subscriptions -> loadSubscriptionShorts(
                api = api,
                page = continuation.page,
                service = service,
                limit = limit,
            ).normalized(limit)
            else -> loadRecommendationShorts(
                api = api,
                cursor = (continuation as? ShortsContinuation.Recommendations)?.cursor,
                service = service,
                limit = limit,
            ).normalized(limit)
                .takeUnless { continuation == null && it.videos.isEmpty() }
                ?: loadSubscriptionShorts(
                    api = api,
                    page = 0,
                    service = service,
                    limit = limit,
                ).normalized(limit)
        }
        activeAccountScope.verify(scope)
        page
    }

    private suspend fun loadCachedFeed(feed: String): List<Video> {
        val scope = activeAccountScope.require()
        val videos = feedVideoDao.get(scope.serverId, scope.accountId, feed).map { it.toDomainVideo() }
        activeAccountScope.verify(scope)
        return videos
    }

    private suspend fun cacheFeed(feed: String, videos: List<Video>, append: Boolean) {
        val scope = activeAccountScope.require()
        val savedAt = System.currentTimeMillis()
        val rows = videos.distinctBy { it.url }.mapIndexed { position, video ->
            video.toFeedEntity(scope, feed, position, savedAt)
        }
        activeAccountScope.verify(scope)
        if (append) {
            feedVideoDao.append(
                scope.serverId,
                scope.accountId,
                feed,
                rows,
                MAX_CACHED_FEED_VIDEOS,
            )
        } else {
            feedVideoDao.replace(
                scope.serverId,
                scope.accountId,
                feed,
                rows.take(MAX_CACHED_FEED_VIDEOS),
            )
        }
    }
}

private suspend fun <T> feedResult(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (failure: Throwable) {
    Result.failure(failure)
}

private const val MAX_CACHED_FEED_VIDEOS = 120
private const val HOME_FEED_KEY = "home"
private const val SUBSCRIPTIONS_FEED_KEY = "subscriptions"
