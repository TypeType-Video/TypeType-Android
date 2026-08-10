package dev.typetype.android.domain.rss

import kotlinx.coroutines.flow.Flow

interface RssRepository {
    fun observeFeeds(): Flow<List<RssFeed>>
    suspend fun refresh(): Result<Unit>
    suspend fun create(draft: RssFeedDraft): Result<RssFeedSecret>
    suspend fun update(feedId: String, draft: RssFeedDraft): Result<Unit>
    suspend fun setEnabled(feedId: String, enabled: Boolean): Result<Unit>
    suspend fun regenerate(feedId: String): Result<RssFeedSecret>
    suspend fun delete(feedId: String): Result<Unit>
}
