package dev.typetype.android.data.rss

import dev.typetype.android.data.account.AccountScope
import dev.typetype.android.data.network.TypeTypeApiHolder
import dev.typetype.android.data.network.dto.RssFeedEnabledRequestDto
import dev.typetype.android.data.network.dto.RssFeedItemDto
import dev.typetype.android.data.network.dto.RssFeedRequestDto
import dev.typetype.android.data.network.requireSuccessfulResponse
import dev.typetype.android.domain.rss.RssFeed
import dev.typetype.android.domain.rss.RssFeedDraft
import dev.typetype.android.domain.rss.RssFeedScope
import dev.typetype.android.domain.rss.RssFeedSecret
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class RssNetworkSource @Inject constructor(
    private val apiHolder: TypeTypeApiHolder,
) : RssDataSource {
    override suspend fun list(scope: AccountScope): List<RssFeed> = withContext(Dispatchers.IO) {
        val response = apiHolder.require(scope).rssFeeds()
        response.requireSuccessfulResponse()
        response.body()?.map(RssFeedItemDto::toDomain) ?: error("Empty RSS feeds response")
    }

    override suspend fun create(scope: AccountScope, draft: RssFeedDraft): RssFeedSecret =
        withContext(Dispatchers.IO) {
            val response = apiHolder.require(scope).createRssFeed(draft.toRequest())
            response.requireSuccessfulResponse()
            val secret = response.body() ?: error("Empty RSS feed response")
            RssFeedSecret(secret.feed.toDomain(), secret.feedUrl)
        }

    override suspend fun update(scope: AccountScope, id: String, draft: RssFeedDraft): RssFeed =
        withContext(Dispatchers.IO) {
            val response = apiHolder.require(scope).updateRssFeed(id, draft.toRequest())
            response.requireSuccessfulResponse()
            response.body()?.toDomain() ?: error("Empty RSS feed response")
        }

    override suspend fun setEnabled(scope: AccountScope, id: String, enabled: Boolean): RssFeed =
        withContext(Dispatchers.IO) {
            val response = apiHolder.require(scope).setRssFeedEnabled(
                id,
                RssFeedEnabledRequestDto(enabled),
            )
            response.requireSuccessfulResponse()
            response.body()?.toDomain() ?: error("Empty RSS feed response")
        }

    override suspend fun regenerate(scope: AccountScope, id: String): RssFeedSecret =
        withContext(Dispatchers.IO) {
            val response = apiHolder.require(scope).regenerateRssFeed(id)
            response.requireSuccessfulResponse()
            val secret = response.body() ?: error("Empty RSS feed response")
            RssFeedSecret(secret.feed.toDomain(), secret.feedUrl)
        }

    override suspend fun delete(scope: AccountScope, id: String) = withContext(Dispatchers.IO) {
        apiHolder.require(scope).deleteRssFeed(id).requireSuccessfulResponse()
    }
}

private fun RssFeedDraft.toRequest() = RssFeedRequestDto(
    name = name.trim(),
    scope = scope.wireValue,
    channelUrls = if (scope == RssFeedScope.Channels) channelUrls.sorted() else emptyList(),
    serviceIds = serviceIds.sorted(),
    includeVideos = includeVideos,
    includeShorts = includeShorts,
    includeLive = includeLive,
    includeUpcoming = includeUpcoming,
)

private fun RssFeedItemDto.toDomain() = RssFeed(
    id = id,
    name = name,
    scope = RssFeedScope.fromWireValue(scope),
    channelUrls = channelUrls,
    serviceIds = serviceIds.toSet(),
    includeVideos = includeVideos,
    includeShorts = includeShorts,
    includeLive = includeLive,
    includeUpcoming = includeUpcoming,
    enabled = enabled,
    createdAt = createdAt,
    updatedAt = updatedAt,
    lastUsedAt = lastUsedAt,
)
