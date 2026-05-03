package dev.typetype.android.domain.feed

interface HomeFeedRepository {
    suspend fun loadHomeRecommendations(): Result<List<Video>>
}
