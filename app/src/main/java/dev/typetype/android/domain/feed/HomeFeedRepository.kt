package dev.typetype.android.domain.feed

data class SubscriptionsPage(
    val videos: List<Video>,
    val hasMore: Boolean,
)

interface HomeFeedRepository {
    suspend fun loadHomeRecommendations(): Result<List<Video>>
    suspend fun loadTrending(): Result<List<Video>>
    suspend fun loadSubscriptionsFeed(page: Int = 0, limit: Int = 30): Result<SubscriptionsPage>
}
