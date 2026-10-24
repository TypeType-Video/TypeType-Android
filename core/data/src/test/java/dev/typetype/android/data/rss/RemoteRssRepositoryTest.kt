package dev.typetype.android.data.rss

import dev.typetype.android.data.account.AccountDao
import dev.typetype.android.data.account.AccountEntity
import dev.typetype.android.data.account.AccountScope
import dev.typetype.android.data.account.AccountScopeProvider
import dev.typetype.android.domain.rss.RssFeed
import dev.typetype.android.domain.rss.RssFeedDraft
import dev.typetype.android.domain.rss.RssFeedScope
import dev.typetype.android.domain.rss.RssFeedSecret
import dev.typetype.android.domain.server.RssCapability
import dev.typetype.android.domain.server.Server
import dev.typetype.android.domain.server.ServerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteRssRepositoryTest {
    @Test
    fun oldServerIsRejectedBeforeAnyRssRequest() = runBlocking {
        val fixture = fixture(rssEnabled = false, isGuest = false)
        val draft = RssFeedDraft(name = "My subscriptions", serviceIds = setOf(0))

        assertTrue(fixture.repository.refresh().isFailure)
        assertTrue(fixture.repository.create(draft).isFailure)
        assertTrue(fixture.repository.update("feed", draft).isFailure)
        assertTrue(fixture.repository.setEnabled("feed", false).isFailure)
        assertTrue(fixture.repository.regenerate("feed").isFailure)
        assertTrue(fixture.repository.delete("feed").isFailure)
        assertEquals(0, fixture.source.calls)
    }

    @Test
    fun guestAccountIsRejectedBeforeAnyRssRequest() = runBlocking {
        val fixture = fixture(rssEnabled = true, isGuest = true)
        val draft = RssFeedDraft(name = "My subscriptions", serviceIds = setOf(0))

        assertTrue(fixture.repository.refresh().isFailure)
        assertTrue(fixture.repository.create(draft).isFailure)
        assertTrue(fixture.repository.update("feed", draft).isFailure)
        assertTrue(fixture.repository.setEnabled("feed", false).isFailure)
        assertTrue(fixture.repository.regenerate("feed").isFailure)
        assertTrue(fixture.repository.delete("feed").isFailure)
        assertEquals(0, fixture.source.calls)
    }

    @Test
    fun cacheNeverCrossesAccountScopes() = runBlocking {
        val fixture = fixture(rssEnabled = true, isGuest = false)
        fixture.source.listResult = listOf(feed("first"))
        fixture.repository.refresh().getOrThrow()
        assertEquals("first", fixture.repository.observeFeeds().first().single().id)

        fixture.scope.current.value = AccountScope("server", "second")
        fixture.accounts.put(account("server", "second", isGuest = false))
        assertTrue(fixture.repository.observeFeeds().first().isEmpty())
        fixture.source.listResult = listOf(feed("second"))
        fixture.repository.refresh().getOrThrow()

        assertEquals("second", fixture.repository.observeFeeds().first().single().id)
    }

    @Test
    fun cacheNeverCrossesServerScopes() = runBlocking {
        val fixture = fixture(rssEnabled = true, isGuest = false)
        fixture.source.listResult = listOf(feed("first"))
        fixture.repository.refresh().getOrThrow()

        fixture.servers.put(server("other", rssEnabled = true))
        fixture.accounts.put(account("other", "first", isGuest = false))
        fixture.scope.current.value = AccountScope("other", "first")

        assertTrue(fixture.repository.observeFeeds().first().isEmpty())
        fixture.source.listResult = listOf(feed("other"))
        fixture.repository.refresh().getOrThrow()
        assertEquals("other", fixture.repository.observeFeeds().first().single().id)
    }

    @Test
    fun completeLifecycleKeepsTheScopedFeedListCurrent() = runBlocking {
        val fixture = fixture(rssEnabled = true, isGuest = false)
        val draft = RssFeedDraft(name = "My subscriptions", serviceIds = setOf(0))

        fixture.repository.refresh().getOrThrow()
        val created = fixture.repository.create(draft).getOrThrow()
        assertEquals("https://example.test/rss/created", created.url)
        assertEquals("My subscriptions", fixture.repository.observeFeeds().first().single().name)

        fixture.repository.update("created", draft.copy(name = "Selected channels")).getOrThrow()
        assertEquals("Selected channels", fixture.repository.observeFeeds().first().single().name)

        fixture.repository.setEnabled("created", false).getOrThrow()
        assertTrue(!fixture.repository.observeFeeds().first().single().enabled)

        val regenerated = fixture.repository.regenerate("created").getOrThrow()
        assertEquals("https://example.test/rss/regenerated", regenerated.url)

        fixture.repository.delete("created").getOrThrow()
        assertTrue(fixture.repository.observeFeeds().first().isEmpty())
    }

    @Test
    fun responseIsDiscardedWhenTheAccountChangesInFlight() = runBlocking {
        val fixture = fixture(rssEnabled = true, isGuest = false)
        fixture.source.onList = {
            fixture.scope.current.value = AccountScope("server", "second")
        }

        assertTrue(fixture.repository.refresh().isFailure)
        assertTrue(fixture.repository.observeFeeds().first().isEmpty())
    }

    private fun fixture(rssEnabled: Boolean, isGuest: Boolean): Fixture {
        val scope = FakeScopeProvider(AccountScope("server", "first"))
        val accounts = FakeAccountDao().apply {
            put(account("server", "first", isGuest))
        }
        val source = FakeRssDataSource()
        val servers = FakeServerRepository(server("server", rssEnabled))
        return Fixture(
            RemoteRssRepository(source, scope, servers, accounts),
            source,
            scope,
            accounts,
            servers,
        )
    }

    private data class Fixture(
        val repository: RemoteRssRepository,
        val source: FakeRssDataSource,
        val scope: FakeScopeProvider,
        val accounts: FakeAccountDao,
        val servers: FakeServerRepository,
    )

    private class FakeScopeProvider(initial: AccountScope) : AccountScopeProvider {
        val current = MutableStateFlow(initial)
        override fun observe(): Flow<AccountScope?> = current
        override suspend fun require(): AccountScope = current.value
        override suspend fun verify(expected: AccountScope) {
            check(expected == current.value)
        }
    }

    private class FakeRssDataSource : RssDataSource {
        var calls = 0
        var listResult = emptyList<RssFeed>()
        var onList: () -> Unit = {}
        override suspend fun list(scope: AccountScope): List<RssFeed> {
            calls += 1
            onList()
            return listResult
        }
        override suspend fun create(scope: AccountScope, draft: RssFeedDraft): RssFeedSecret {
            calls += 1
            val created = feed("created").copy(name = draft.name)
            listResult = listResult + created
            return RssFeedSecret(created, "https://example.test/rss/created")
        }
        override suspend fun update(
            scope: AccountScope,
            id: String,
            draft: RssFeedDraft,
        ): RssFeed {
            calls += 1
            return requireFeed(id).copy(name = draft.name, updatedAt = 3).also(::replace)
        }
        override suspend fun setEnabled(
            scope: AccountScope,
            id: String,
            enabled: Boolean,
        ): RssFeed {
            calls += 1
            return requireFeed(id).copy(enabled = enabled, updatedAt = 4).also(::replace)
        }
        override suspend fun regenerate(scope: AccountScope, id: String): RssFeedSecret {
            calls += 1
            return RssFeedSecret(requireFeed(id), "https://example.test/rss/regenerated")
        }
        override suspend fun delete(scope: AccountScope, id: String) {
            calls += 1
            listResult = listResult.filterNot { it.id == id }
        }
        private fun requireFeed(id: String) = requireNotNull(listResult.firstOrNull { it.id == id })
        private fun replace(feed: RssFeed) {
            listResult = listResult.filterNot { it.id == feed.id } + feed
        }
    }

    private class FakeServerRepository(server: Server) : ServerRepository {
        private val servers = mutableMapOf(server.id to server)
        fun put(server: Server) {
            servers[server.id] = server
        }
        override fun observeServers(): Flow<List<Server>> = flowOf(servers.values.toList())
        override fun observeCurrentServer(): Flow<Server?> = flowOf(servers.values.firstOrNull())
        override suspend fun getServer(id: String): Server? = servers[id]
        override suspend fun addServer(server: Server) = Unit
        override suspend fun deleteServer(id: String) = Unit
        override suspend fun setCurrentServer(id: String) = Unit
        override suspend fun clearCurrentServer() = Unit
    }

    private class FakeAccountDao : AccountDao {
        private val accounts = mutableMapOf<Pair<String, String>, AccountEntity>()
        fun put(account: AccountEntity) {
            accounts[account.serverId to account.accountId] = account
        }
        override fun observeAll() = flowOf(accounts.values.toList())
        override fun observeForServer(serverId: String) =
            flowOf(accounts.values.filter { it.serverId == serverId })
        override fun observe(serverId: String, accountId: String) =
            flowOf(accounts[serverId to accountId])
        override suspend fun get(serverId: String, accountId: String) =
            accounts[serverId to accountId]
        override fun observeSessionGeneration(serverId: String, accountId: String) = flowOf(0L)
        override suspend fun upsert(account: AccountEntity) = Unit
        override suspend fun updateLastUsed(serverId: String, accountId: String, lastUsedAt: Long) = Unit
        override suspend fun delete(serverId: String, accountId: String) = Unit
    }

    private companion object {
        fun account(serverId: String, id: String, isGuest: Boolean) = AccountEntity(
            serverId, id, null, null, null, null, null, isGuest, 0, 0,
        )
        fun server(id: String, rssEnabled: Boolean) = Server(
            id = id,
            baseUrl = "https://example.test/api/",
            displayName = "Test",
            addedAt = 0,
            rss = RssCapability(enabled = rssEnabled),
        )
        fun feed(id: String) = RssFeed(
            id, id, RssFeedScope.All, emptyList(), setOf(0), true, true, true, true,
            true, 1, 2, null,
        )
    }
}
