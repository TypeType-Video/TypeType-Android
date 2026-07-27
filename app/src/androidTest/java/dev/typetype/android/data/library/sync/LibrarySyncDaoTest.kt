package dev.typetype.android.data.library.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.typetype.android.data.account.AccountEntity
import dev.typetype.android.data.database.TypeTypeDatabase
import dev.typetype.android.data.server.ServerEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibrarySyncDaoTest {
    private lateinit var database: TypeTypeDatabase
    private lateinit var dao: LibrarySyncDao

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
        dao = database.librarySyncDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun newerRefreshRejectsLateResultFromOlderGeneration() = runBlocking {
        val first = dao.begin(SERVER_ID, ACCOUNT_ID, COLLECTION, 10L)
        val second = dao.begin(SERVER_ID, ACCOUNT_ID, COLLECTION, 20L)

        assertFalse(dao.isCurrent(SERVER_ID, ACCOUNT_ID, COLLECTION, first))
        assertTrue(dao.isCurrent(SERVER_ID, ACCOUNT_ID, COLLECTION, second))
        assertEquals(
            0,
            dao.completeFailure(
                SERVER_ID,
                ACCOUNT_ID,
                COLLECTION,
                first,
                30L,
                "server_failure",
                503,
                "old-request",
            ),
        )
        assertEquals(1, dao.completeSuccess(SERVER_ID, ACCOUNT_ID, COLLECTION, second, 40L))

        val state = requireNotNull(dao.get(SERVER_ID, ACCOUNT_ID, COLLECTION))
        assertEquals(second, state.refreshGeneration)
        assertEquals(40L, state.lastSuccessAtMillis)
        assertNull(state.lastFailureAtMillis)
        assertNull(state.failureCode)
        assertNull(state.requestId)
    }

    private companion object {
        const val SERVER_ID = "server-a"
        const val ACCOUNT_ID = "account-a"
        const val COLLECTION = "history"
    }
}
