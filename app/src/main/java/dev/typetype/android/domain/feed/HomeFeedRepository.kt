package dev.typetype.android.domain.feed

interface HomeFeedRepository {
    suspend fun loadHomeRecommendations(): Result<List<Video>>
    suspend fun loadTrending(): Result<List<Video>>
    suspend fun loadSubscriptionsFeed(): Result<List<Video>>
}
