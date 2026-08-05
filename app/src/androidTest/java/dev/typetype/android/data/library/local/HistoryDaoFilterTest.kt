package dev.typetype.android.data.library.local

import android.content.Context
import androidx.paging.PagingSource
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.typetype.android.data.account.AccountEntity
import dev.typetype.android.data.database.TypeTypeDatabase
import dev.typetype.android.data.server.ServerEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HistoryDaoFilterTest {
    private lateinit var database: TypeTypeDatabase
    private lateinit var dao: HistoryDao

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, TypeTypeDatabase::class.java).build()
        database.serverDao().upsert(
            ServerEntity(
                id = SERVER_ID,
                baseUrl = "https://instance.example/api/",
                displayName = "TypeType",
                addedAt = 1L,
            ),
        )
        database.accountDao().upsert(
            AccountEntity(
                serverId = SERVER_ID,
                accountId = ACCOUNT_ID,
                publicUsername = null,
                role = null,
                avatarUrl = null,
                avatarType = null,
                avatarCode = null,
                isGuest = false,
                lastUsedAt = 1L,
                sessionGeneration = 1L,
            ),
        )
        dao = database.historyDao()
        dao.upsertAll(
            listOf(
                history("early", "Alpha", "Channel", 1_000L),
                history("match", "Target video", "Channel", 2_000L),
                history("channel", "Other", "Target creator", 2_500L),
                history("late", "Target late", "Channel", 5_000L),
            ),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun pagingSourceCombinesSearchAndDateRange() = runBlocking {
        val rows = load(search = "target", fromMillis = 1_500L, toMillis = 3_000L)

        assertEquals(listOf("channel", "match"), rows.map(HistoryEntity::id))
    }

    @Test
    fun replacingFilteredResultsKeepsUnrelatedCachedRows() = runBlocking {
        dao.deleteMatching(
            serverId = SERVER_ID,
            accountId = ACCOUNT_ID,
            search = "target",
            fromMillis = 1_500L,
            toMillis = 3_000L,
        )

        assertEquals(listOf("late", "early"), load().map(HistoryEntity::id))
    }

    private suspend fun load(
        search: String = "",
        fromMillis: Long? = null,
        toMillis: Long? = null,
    ): List<HistoryEntity> {
        val source = dao.pagingSource(
            serverId = SERVER_ID,
            accountId = ACCOUNT_ID,
            search = search,
            orderKey = 0,
            fromMillis = fromMillis,
            toMillis = toMillis,
        )
        val result = source.load(
            PagingSource.LoadParams.Refresh(
                key = null,
                loadSize = 20,
                placeholdersEnabled = false,
            ),
        )
        return (result as PagingSource.LoadResult.Page).data
    }

    private fun history(id: String, title: String, channel: String, watchedAt: Long) =
        HistoryEntity(
            serverId = SERVER_ID,
            accountId = ACCOUNT_ID,
            id = id,
            url = "https://video.example/$id",
            title = title,
            thumbnailUrl = "",
            channelName = channel,
            durationSeconds = 60L,
            progressSeconds = 0L,
            watchedAtMillis = watchedAt,
        )

    private companion object {
        const val SERVER_ID = "server-a"
        const val ACCOUNT_ID = "account-a"
    }
}
