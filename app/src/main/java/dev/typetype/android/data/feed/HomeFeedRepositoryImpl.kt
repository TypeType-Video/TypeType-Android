package dev.typetype.android.data.feed

import dev.typetype.android.data.network.TypeTypeApiHolder
import dev.typetype.android.data.network.dto.VideoItem
import dev.typetype.android.domain.feed.HomeFeedRepository
import dev.typetype.android.domain.feed.Video
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class HomeFeedRepositoryImpl @Inject constructor(
    private val apiHolder: TypeTypeApiHolder,
) : HomeFeedRepository {

    override suspend fun loadHomeRecommendations(): Result<List<Video>> = runCatching {
        val api = apiHolder.require()
        val response = withContext(Dispatchers.IO) { api.homeRecommendations() }
        if (!response.isSuccessful) {
            error("Recommendations failed (HTTP ${response.code()})")
        }
        val body = response.body() ?: error("Empty recommendations body")
        body.items.map { it.toDomain() }
    }

    private fun VideoItem.toDomain(): Video = Video(
        id = id,
        url = url,
        title = title,
        thumbnailUrl = thumbnailUrl,
        uploaderName = uploaderName,
        uploaderUrl = uploaderUrl,
        uploaderAvatarUrl = uploaderAvatarUrl,
        uploaderVerified = uploaderVerified,
        durationSeconds = duration,
        viewCount = viewCount,
        uploadedAtMillis = uploaded,
        isShortFormContent = isShortFormContent,
        shortDescription = shortDescription,
    )
}
