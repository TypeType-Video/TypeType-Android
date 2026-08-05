package dev.typetype.android.data.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class YoutubeTakeoutImportMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        instrumentation = InstrumentationRegistry.getInstrumentation(),
        databaseClass = TypeTypeDatabase::class.java,
    )

    @Test
    fun migration20To21CreatesScopedImportQueueWithCascade() {
        helper.createDatabase(DATABASE_NAME, 20).apply {
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

        val migrated = helper.runMigrationsAndValidate(
            DATABASE_NAME,
            21,
            true,
            TypeTypeDatabaseMigrations.MIGRATION_20_21,
        )
        migrated.execSQL("PRAGMA foreign_keys = ON")
        migrated.execSQL(
            "INSERT INTO youtube_takeout_imports(" +
                "serverId, accountId, sessionGeneration, requestId, workId, documentUri, " +
                "displayName, status, warningCount, errorCount, collectionsRefreshed, " +
                "createdAtMillis, updatedAtMillis) " +
                "VALUES('instance-a', 'account-a', 4, 'request-a', 'work-a', " +
                "'content://takeout', 'takeout.zip', 'QUEUED', 0, 0, 0, 3, 3)",
        )
        migrated.query("SELECT displayName, sessionGeneration FROM youtube_takeout_imports").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("takeout.zip", cursor.getString(0))
            assertEquals(4L, cursor.getLong(1))
        }

        migrated.execSQL("DELETE FROM accounts WHERE serverId = 'instance-a' AND accountId = 'account-a'")
        migrated.query("SELECT COUNT(*) FROM youtube_takeout_imports").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
        migrated.close()
    }

    private companion object {
        const val DATABASE_NAME = "youtube-takeout-migration.db"
    }
}
