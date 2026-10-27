package dev.typetype.android.data.rss

import dev.typetype.android.data.account.AccountDao
import dev.typetype.android.data.account.AccountScope
import dev.typetype.android.data.account.AccountScopedValue
import dev.typetype.android.data.account.AccountScopeProvider
import dev.typetype.android.domain.rss.RssFeed
import dev.typetype.android.domain.rss.RssFeedDraft
import dev.typetype.android.domain.rss.RssFeedSecret
import dev.typetype.android.domain.rss.RssRepository
import dev.typetype.android.domain.server.ServerRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class RemoteRssRepository @Inject constructor(
    private val network: RssDataSource,
    private val activeAccountScope: AccountScopeProvider,
    private val serverRepository: ServerRepository,
    private val accountDao: AccountDao,
) : RssRepository {
    private val state = MutableStateFlow<AccountScopedValue<List<RssFeed>>?>(null)
    private val mutex = Mutex()

    override fun observeFeeds(): Flow<List<RssFeed>> =
        combine(activeAccountScope.observe(), state) { scope, cached ->
            cached?.value?.takeIf { cached.scope == scope }.orEmpty()
        }

    override suspend fun refresh(): Result<Unit> = rssResult {
        mutex.withLock {
            val scope = requireAvailableScope()
            replace(scope, network.list(scope))
        }
    }

    override suspend fun create(draft: RssFeedDraft): Result<RssFeedSecret> = rssResult {
        mutex.withLock {
            val scope = requireAvailableScope()
            val secret = network.create(scope, draft)
            upsert(scope, secret.feed)
            secret
        }
    }

    override suspend fun update(feedId: String, draft: RssFeedDraft): Result<Unit> = rssResult {
        mutex.withLock {
            val scope = requireAvailableScope()
            upsert(scope, network.update(scope, feedId, draft))
        }
    }

    override suspend fun setEnabled(feedId: String, enabled: Boolean): Result<Unit> = rssResult {
        mutex.withLock {
            val scope = requireAvailableScope()
            upsert(scope, network.setEnabled(scope, feedId, enabled))
        }
    }

    override suspend fun regenerate(feedId: String): Result<RssFeedSecret> = rssResult {
        mutex.withLock {
            val scope = requireAvailableScope()
            val secret = network.regenerate(scope, feedId)
            upsert(scope, secret.feed)
            secret
        }
    }

    override suspend fun delete(feedId: String): Result<Unit> = rssResult {
        mutex.withLock {
            val scope = requireAvailableScope()
            network.delete(scope, feedId)
            activeAccountScope.verify(scope)
            val feeds = state.value?.takeIf { it.scope == scope }?.value.orEmpty()
            state.value = AccountScopedValue(scope, feeds.filterNot { it.id == feedId })
        }
    }

    private suspend fun requireAvailableScope(): AccountScope {
        val scope = activeAccountScope.require()
        val server = serverRepository.getServer(scope.serverId)
        check(server?.rss?.enabled == true) { "RSS feeds are unavailable on this instance" }
        val account = accountDao.get(scope.serverId, scope.accountId)
        check(account?.isGuest == false) { "Guest accounts cannot manage RSS feeds" }
        return scope
    }

    private suspend fun replace(scope: AccountScope, feeds: List<RssFeed>) {
        activeAccountScope.verify(scope)
        state.value = AccountScopedValue(scope, feeds)
    }

    private suspend fun upsert(scope: AccountScope, feed: RssFeed) {
        activeAccountScope.verify(scope)
        val feeds = state.value?.takeIf { it.scope == scope }?.value.orEmpty()
        state.value = AccountScopedValue(
            scope,
            (feeds.filterNot { it.id == feed.id } + feed).sortedByDescending { it.updatedAt },
        )
    }
}

private suspend fun <T> rssResult(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (failure: Throwable) {
    Result.failure(failure)
}
