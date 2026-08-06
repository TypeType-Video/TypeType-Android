package dev.typetype.android.data.imports

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.typetype.android.data.account.AccountEntity
import dev.typetype.android.data.database.TypeTypeDatabase
import dev.typetype.android.data.server.ServerEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class YoutubeTakeoutImportPersistenceTest {
    @Test
    fun queuedJobSurvivesDatabaseReopen() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase(DATABASE_NAME)
        val first = openDatabase(context)
        first.serverDao().upsert(
            ServerEntity(SERVER_ID, "https://instance.example/api/", "TypeType", 1L),
        )
        first.accountDao().upsert(account())
        first.youtubeTakeoutImportDao().upsert(entry())
        first.close()

        val reopened = openDatabase(context)
        try {
            val restored = reopened.youtubeTakeoutImportDao().getByWorkId(WORK_ID)
            assertEquals("QUEUED", restored?.status)
            assertEquals("takeout.zip", restored?.displayName)
            assertEquals(SESSION_GENERATION, restored?.sessionGeneration)
        } finally {
            reopened.close()
            context.deleteDatabase(DATABASE_NAME)
        }
    }

    private fun openDatabase(context: Context): TypeTypeDatabase = Room.databaseBuilder(
        context,
        TypeTypeDatabase::class.java,
        DATABASE_NAME,
    ).build()

    private fun account() = AccountEntity(
        serverId = SERVER_ID,
        accountId = ACCOUNT_ID,
        publicUsername = null,
        role = null,
        avatarUrl = null,
        avatarType = null,
        avatarCode = null,
        isGuest = false,
        lastUsedAt = 1L,
        sessionGeneration = SESSION_GENERATION,
    )

    private fun entry() = YoutubeTakeoutImportEntity(
        serverId = SERVER_ID,
        accountId = ACCOUNT_ID,
        sessionGeneration = SESSION_GENERATION,
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
        const val DATABASE_NAME = "takeout-persistence-test.db"
        const val SERVER_ID = "server-a"
        const val ACCOUNT_ID = "account-a"
        const val SESSION_GENERATION = 2L
        const val WORK_ID = "00000000-0000-0000-0000-000000000001"
    }
}
