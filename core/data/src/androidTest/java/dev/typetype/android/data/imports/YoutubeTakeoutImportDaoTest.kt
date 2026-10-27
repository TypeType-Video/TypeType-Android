package dev.typetype.android.data.imports

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
class YoutubeTakeoutImportDaoTest {
    private lateinit var database: TypeTypeDatabase
    private lateinit var dao: YoutubeTakeoutImportDao

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
        dao = database.youtubeTakeoutImportDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun cancellationWinsOverLateWorkerUpdates() = runBlocking {
        dao.upsert(entry())

        assertEquals(1, dao.cancel(WORK_ID, YoutubeTakeoutFailureCodes.Cancelled, 2L))
        assertEquals(0, dao.attachServerJob(WORK_ID, "job", "parsing", 10, 3L))
        assertEquals(0, dao.updateProgress(WORK_ID, "IMPORTING", "history", 80, 4L))
        assertEquals(0, dao.savePreview(WORK_ID, 1, 2, 3, 4, 5, 6, 7, 8, 5L))
        assertEquals(0, dao.complete(WORK_ID, 10, 2, 1, 3, 4, 6L))
        assertEquals(0, dao.fail(WORK_ID, "late_failure", "request", 7L))
        assertEquals(0, dao.retrying(WORK_ID, "late_retry", "request", 8L))

        val cancelled = dao.getByWorkId(WORK_ID)
        assertEquals("CANCELLED", cancelled?.status)
        assertEquals(null, cancelled?.serverJobId)
        assertEquals(2L, cancelled?.updatedAtMillis)
    }

    private fun entry() = YoutubeTakeoutImportEntity(
        serverId = SERVER_ID,
        accountId = ACCOUNT_ID,
        sessionGeneration = 2L,
        requestId = "request-a",
        workId = WORK_ID,
        documentUri = "content://takeout/archive",
        displayName = "takeout.zip",
        sizeBytes = 1_024L,
        serverJobId = null,
        status = "QUEUED",
        phase = null,
        progressPercent = null,
        previewSubscriptions = null,
        previewPlaylists = null,
        previewPlaylistItems = null,
        previewFavorites = null,
        previewWatchLater = null,
        previewHistory = null,
        importedCount = null,
        skippedCount = null,
        failedCount = null,
        warningCount = 0,
        errorCount = 0,
        failureCode = null,
        failureRequestId = null,
        collectionsRefreshed = false,
        createdAtMillis = 1L,
        updatedAtMillis = 1L,
    )

    private companion object {
        const val SERVER_ID = "server-a"
        const val ACCOUNT_ID = "account-a"
        const val WORK_ID = "00000000-0000-0000-0000-000000000001"
    }
}
