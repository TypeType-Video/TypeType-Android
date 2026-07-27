package dev.typetype.android.domain.feed

data class SubscriptionsPage(
    val videos: List<Video>,
    val nextCursor: String?,
    val generation: Long,
    val generatedAtMillis: Long,
    val refreshing: Boolean,
) {
    val hasMore: Boolean
        get() = nextCursor != null
}

data class HomeRecommendationsPage(
    val videos: List<Video>,
    val nextCursor: String?,
)

interface HomeFeedRepository {
    suspend fun loadCachedHomeFeed(): List<Video>
    suspend fun loadCachedSubscriptionsFeed(): List<Video>
    suspend fun cacheHomeFeed(videos: List<Video>, append: Boolean)
    suspend fun cacheSubscriptionsFeed(videos: List<Video>, append: Boolean)
    suspend fun loadHomeRecommendations(
        cursor: String? = null,
        limit: Int = 20,
    ): Result<HomeRecommendationsPage>
    suspend fun loadTrending(): Result<List<Video>>
    suspend fun loadSubscriptionsFeed(
        cursor: String? = null,
        limit: Int = 30,
        expectedGeneration: Long? = null,
    ): Result<SubscriptionsPage>
}
