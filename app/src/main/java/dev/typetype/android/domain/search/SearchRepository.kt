package dev.typetype.android.domain.search

import dev.typetype.android.domain.feed.Video

interface SearchRepository {
    suspend fun search(query: String, service: Int = 0): Result<List<Video>>
}
