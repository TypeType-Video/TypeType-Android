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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProgressOutboxDaoTest {
    private lateinit var database: TypeTypeDatabase
    private lateinit var dao: ProgressOutboxDao

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, TypeTypeDatabase::class.java).build()
        database.serverDao().upsert(
            ServerEntity(SERVER_ID, "https://instance.example/api/", "TypeType", 1L),
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
                sessionGeneration = 2L,
            ),
        )
        dao = database.progressOutboxDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun deletingDisabledTrackingGenerationPreservesOtherGenerations() = runBlocking {
        dao.upsert(entry("first", 2L))
        dao.upsert(entry("second", 2L))
        dao.upsert(entry("future", 3L))

        dao.deleteGeneration(SERVER_ID, ACCOUNT_ID, 2L)

        assertEquals(emptyList<ProgressOutboxEntity>(), dao.pending(SERVER_ID, ACCOUNT_ID, 2L, 20))
        assertEquals(listOf("future"), dao.pending(SERVER_ID, ACCOUNT_ID, 3L, 20).map { it.videoUrl })
    }

    @Test
    fun disablingTrackingInteractivelyClearsEveryPendingSessionGeneration() = runBlocking {
        dao.upsert(entry("current", 2L))
        dao.upsert(entry("stale", 1L))

        dao.deleteAllForScope(SERVER_ID, ACCOUNT_ID)

        assertEquals(emptyList<ProgressOutboxEntity>(), dao.pending(SERVER_ID, ACCOUNT_ID, 2L, 20))
        assertEquals(emptyList<ProgressOutboxEntity>(), dao.pending(SERVER_ID, ACCOUNT_ID, 1L, 20))
    }

    private fun entry(videoUrl: String, generation: Long) = ProgressOutboxEntity(
        serverId = SERVER_ID,
        accountId = ACCOUNT_ID,
        videoUrl = videoUrl,
        positionMillis = 5_000L,
        sessionGeneration = generation,
        updatedAtMillis = generation,
    )

    private companion object {
        const val SERVER_ID = "server-a"
        const val ACCOUNT_ID = "account-a"
    }
}
