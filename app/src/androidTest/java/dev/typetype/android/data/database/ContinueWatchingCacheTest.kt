package dev.typetype.android.data.database

import android.content.Context
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
class ContinueWatchingCacheTest {
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
    fun continueWatchingMatchesFrontendCompletionRulesAndRecency() = runBlocking {
        val dao = database.historyDao()
        dao.upsert(history(id = "not-started", duration = 120, progress = 0, watchedAt = 6))
        dao.upsert(history(id = "long-finished", duration = 120, progress = 60, watchedAt = 5))
        dao.upsert(history(id = "short-finished", duration = 60, progress = 54, watchedAt = 4))
        dao.upsert(history(id = "older-active", duration = 60, progress = 53, watchedAt = 2))
        dao.upsert(history(id = "newer-active", duration = 120, progress = 59, watchedAt = 3))

        val items = dao.observeContinueWatching(SERVER_ID, ACCOUNT_ID, limit = 2).first()

        assertEquals(listOf("newer-active", "older-active"), items.map { it.id })
    }

    private fun history(
        id: String,
        duration: Long,
        progress: Long,
        watchedAt: Long,
    ) = HistoryEntity(
        serverId = SERVER_ID,
        accountId = ACCOUNT_ID,
        id = id,
        url = "https://video.example/$id",
        title = id,
        thumbnailUrl = "",
        channelName = "Channel",
        durationSeconds = duration,
        progressSeconds = progress,
        watchedAtMillis = watchedAt,
    )

    private companion object {
        const val SERVER_ID = "server"
        const val ACCOUNT_ID = "account"
    }
}
