package dev.typetype.android.data.database

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlaybackResumeMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        instrumentation = InstrumentationRegistry.getInstrumentation(),
        databaseClass = TypeTypeDatabase::class.java,
    )

    private val context: Context = ApplicationProvider.getApplicationContext()
    private var database: TypeTypeDatabase? = null

    @After
    fun closeDatabase() {
        database?.close()
        context.deleteDatabase(DATABASE_NAME)
    }

    @Test
    fun migrationCreatesAccountScopedPlaybackResumeWithCascade() {
        helper.createDatabase(DATABASE_NAME, 15).apply {
            execSQL(
                "INSERT INTO servers(id, baseUrl, displayName, addedAt) " +
                    "VALUES('instance-a', 'https://example.test/api', 'TypeType', 1)",
            )
            execSQL(
                "INSERT INTO accounts(serverId, accountId, isGuest, lastUsedAt, sessionGeneration) " +
                    "VALUES('instance-a', 'account-a', 0, 2, 4)",
            )
            close()
        }

        database = Room.databaseBuilder(context, TypeTypeDatabase::class.java, DATABASE_NAME)
            .addMigrations(*TypeTypeDatabaseMigrations.ALL)
            .build()
        val sqlite = requireNotNull(database).openHelper.writableDatabase
        sqlite.execSQL(
            "INSERT INTO playback_resume(" +
                "serverId, accountId, videoUrl, positionMillis, updatedAtMillis" +
                ") VALUES('instance-a', 'account-a', 'video-a', 42000, 3)",
        )

        sqlite.query("SELECT videoUrl, positionMillis FROM playback_resume").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("video-a", cursor.getString(0))
            assertEquals(42_000L, cursor.getLong(1))
        }

        sqlite.execSQL("DELETE FROM accounts WHERE serverId = 'instance-a' AND accountId = 'account-a'")
        sqlite.query("SELECT COUNT(*) FROM playback_resume").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
    }

    private companion object {
        const val DATABASE_NAME = "playback-resume-migration.db"
    }
}
