package dev.typetype.android.data.database

import android.content.Context
import androidx.paging.PagingSource
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.typetype.android.data.account.AccountEntity
import dev.typetype.android.data.library.local.HistoryEntity
import dev.typetype.android.data.server.ServerEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HistoryPagingDaoTest {
    private lateinit var database: TypeTypeDatabase

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, TypeTypeDatabase::class.java).build()
        database.serverDao().upsert(
            ServerEntity(SERVER_ID, "https://instance.example/api/", "Instance", 1L),
        )
        database.accountDao().upsert(
            AccountEntity(
                serverId = SERVER_ID,
                accountId = ACCOUNT_ID,
                publicUsername = "user",
                role = "user",
                avatarUrl = null,
                avatarType = null,
                avatarCode = null,
                isGuest = false,
                lastUsedAt = 1L,
                sessionGeneration = 1L,
            ),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun pagingFiltersSortsAndKeepsMembershipQueriesBounded() = runBlocking {
        val dao = database.historyDao()
        dao.upsertAll(
            listOf(
                history("z", "Zebra", "Channel", progress = 89L, watchedAt = 30L),
                history("a", "Alpha", "Channel", progress = 90L, watchedAt = 20L),
                history("b", "Beta", "Alpha creator", progress = 0L, watchedAt = 10L),
            ),
        )

        val source = dao.pagingSource(
            SERVER_ID,
            ACCOUNT_ID,
            "alpha",
            fromMillis = null,
            toMillis = null,
            orderKey = 2,
        )
        val result = source.load(
            PagingSource.LoadParams.Refresh(
                key = null,
                loadSize = 20,
                placeholdersEnabled = false,
            ),
        ) as PagingSource.LoadResult.Page

        assertEquals(listOf("Alpha", "Beta"), result.data.map { it.title })
        assertEquals(3, dao.observeCount(SERVER_ID, ACCOUNT_ID).first())
        assertEquals(listOf("url-a"), dao.observeWatchedUrls(SERVER_ID, ACCOUNT_ID).first())
    }

    private fun history(
        id: String,
        title: String,
        channel: String,
        progress: Long,
        watchedAt: Long,
    ) = HistoryEntity(
        serverId = SERVER_ID,
        accountId = ACCOUNT_ID,
        id = id,
        url = "url-$id",
        title = title,
        thumbnailUrl = "",
        channelName = channel,
        durationSeconds = 100L,
        progressSeconds = progress,
        watchedAtMillis = watchedAt,
    )

    private companion object {
        const val SERVER_ID = "server"
        const val ACCOUNT_ID = "account"
    }
}
