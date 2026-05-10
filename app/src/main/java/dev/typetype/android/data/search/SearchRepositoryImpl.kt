package dev.typetype.android.data.search

import dev.typetype.android.data.network.TypeTypeApiHolder
import dev.typetype.android.domain.feed.Video
import dev.typetype.android.domain.search.SearchRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchRepositoryImpl @Inject constructor(
    private val apiHolder: TypeTypeApiHolder,
) : SearchRepository {

    override suspend fun search(query: String, service: Int): Result<List<Video>> = runCatching {
        val api = apiHolder.require()
        val response = withContext(Dispatchers.IO) { api.search(query = query, service = service) }
        if (!response.isSuccessful) error("Search failed (HTTP ${response.code()})")
        val body = response.body() ?: error("Empty search body")
        body.items.map { item ->
            Video(
                id = item.id,
                url = item.url,
                title = item.title,
                thumbnailUrl = item.thumbnailUrl,
                uploaderName = item.uploaderName,
                uploaderUrl = item.uploaderUrl,
                uploaderAvatarUrl = item.uploaderAvatarUrl,
                uploaderVerified = item.uploaderVerified,
                durationSeconds = item.duration,
                viewCount = item.viewCount,
                uploadedAtMillis = item.uploaded,
                isShortFormContent = item.isShortFormContent,
                shortDescription = item.shortDescription,
            )
        }
    }

    override suspend fun suggestions(query: String, service: Int): Result<List<String>> = runCatching {
        val api = apiHolder.require()
        val response = withContext(Dispatchers.IO) {
            api.searchSuggestions(query = query, service = service)
        }
        if (!response.isSuccessful) error("Suggestions failed (HTTP ${response.code()})")
        response.body() ?: emptyList()
    }
}
