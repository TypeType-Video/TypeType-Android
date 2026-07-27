package dev.typetype.android.data.database

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
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
class TypeTypeDatabaseMigrationTest {
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
    fun migratesVersion5ToCurrentWithoutLosingServers() {
        helper.createDatabase(DATABASE_NAME, 5).apply {
            insertVersion5Rows()
            close()
        }

        database = Room.databaseBuilder(context, TypeTypeDatabase::class.java, DATABASE_NAME)
            .addMigrations(*TypeTypeDatabaseMigrations.ALL)
            .build()
        val sqlite = requireNotNull(database).openHelper.writableDatabase

        sqlite.query("SELECT displayName, version, apiVersion FROM servers WHERE id = 'instance-a'")
            .use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Local TypeType", cursor.getString(0))
                assertEquals("", cursor.getString(1))
                assertEquals(0, cursor.getInt(2))
            }
        sqlite.query("PRAGMA table_info(accounts)").use { cursor ->
            assertTrue(cursor.count > 0)
            assertTrue(cursor.hasColumn("sessionGeneration"))
        }
        sqlite.query("PRAGMA table_info(downloads)").use { cursor ->
            assertTrue(cursor.hasColumn("sessionGeneration"))
        }
        sqlite.query("PRAGMA table_info(progress_outbox)").use { cursor ->
            assertTrue(cursor.count > 0)
        }
        sqlite.query("SELECT COUNT(*) FROM favorites").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
        sqlite.execSQL(
            "INSERT INTO accounts(serverId, accountId, isGuest, lastUsedAt, sessionGeneration) " +
                "VALUES('instance-a', 'account-a', 0, 1, 7)",
        )
        sqlite.execSQL(
            "INSERT INTO progress_outbox(" +
                "serverId, accountId, videoUrl, positionMillis, sessionGeneration, updatedAtMillis" +
                ") VALUES('instance-a', 'account-a', 'video-a', 1000, 7, 2)",
        )
        sqlite.execSQL(
            "INSERT OR REPLACE INTO progress_outbox(" +
                "serverId, accountId, videoUrl, positionMillis, sessionGeneration, updatedAtMillis" +
                ") VALUES('instance-a', 'account-a', 'video-a', 2000, 7, 3)",
        )
        sqlite.query("SELECT positionMillis FROM progress_outbox WHERE videoUrl = 'video-a'")
            .use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(2000L, cursor.getLong(0))
            }
    }

    @Test
    fun repairsDraftVersion10ServerSchemaWithoutLosingScope() {
        helper.createDatabase(DATABASE_NAME, 10).apply {
            execSQL("PRAGMA foreign_keys = OFF")
            execSQL("DROP TABLE `servers`")
            execSQL(DRAFT_VERSION_10_SERVERS)
            execSQL(
                "INSERT INTO servers(id, baseUrl, displayName, addedAt, tagline, revision) " +
                    "VALUES('instance-a', 'https://example.test/api', 'Draft server', 1, 'Hi', 'abc')",
            )
            execSQL(
                "INSERT INTO accounts(serverId, accountId, isGuest, lastUsedAt) " +
                    "VALUES('instance-a', 'account-a', 0, 2)",
            )
            execSQL("PRAGMA foreign_keys = ON")
            close()
        }

        database = Room.databaseBuilder(context, TypeTypeDatabase::class.java, DATABASE_NAME)
            .addMigrations(*TypeTypeDatabaseMigrations.ALL)
            .build()
        val sqlite = requireNotNull(database).openHelper.writableDatabase

        sqlite.query("SELECT version, apiVersion, localLoginEnabled FROM servers").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("", cursor.getString(0))
            assertEquals(0, cursor.getInt(1))
            assertEquals(1, cursor.getInt(2))
        }
        sqlite.query("SELECT sessionGeneration FROM accounts").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
    }

    @Test
    fun migratesEveryRetainedSchemaToCurrent() {
        for (version in FIRST_RETAINED_VERSION..CURRENT_VERSION) {
            val name = "migration-$version.db"
            helper.createDatabase(name, version).close()
            val migrated = Room.databaseBuilder(context, TypeTypeDatabase::class.java, name)
                .addMigrations(*TypeTypeDatabaseMigrations.ALL)
                .build()
            migrated.openHelper.writableDatabase.query("SELECT 1").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(1, cursor.getInt(0))
            }
            migrated.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun migratesVersion12WithScopedLibraryFreshnessAndCascade() {
        helper.createDatabase(DATABASE_NAME, 12).apply {
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
            "INSERT INTO library_sync_state(" +
                "serverId, accountId, collection, refreshGeneration, lastAttemptAtMillis, " +
                "lastSuccessAtMillis, failureCode" +
                ") VALUES('instance-a', 'account-a', 'history', 2, 10, 11, NULL)",
        )

        sqlite.query(
            "SELECT refreshGeneration, lastSuccessAtMillis FROM library_sync_state " +
                "WHERE serverId = 'instance-a' AND accountId = 'account-a'",
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(2L, cursor.getLong(0))
            assertEquals(11L, cursor.getLong(1))
        }

        sqlite.execSQL("DELETE FROM accounts WHERE serverId = 'instance-a' AND accountId = 'account-a'")
        sqlite.query("SELECT COUNT(*) FROM library_sync_state").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
    }

    @Test
    fun migratesVersion13WithDurableScopedLibraryMutations() {
        helper.createDatabase(DATABASE_NAME, 13).apply {
            execSQL(
                "INSERT INTO servers(id, baseUrl, displayName, addedAt) " +
                    "VALUES('instance-a', 'https://example.test/api', 'TypeType', 1)",
            )
            execSQL(
                "INSERT INTO accounts(serverId, accountId, isGuest, lastUsedAt, sessionGeneration) " +
                    "VALUES('instance-a', 'account-a', 0, 2, 4)",
            )
            execSQL(
                "INSERT INTO favorites(serverId, accountId, videoUrl, favoritedAtMillis) " +
                    "VALUES('instance-a', 'account-a', 'video-a', 3)",
            )
            close()
        }

        database = Room.databaseBuilder(context, TypeTypeDatabase::class.java, DATABASE_NAME)
            .addMigrations(*TypeTypeDatabaseMigrations.ALL)
            .build()
        val sqlite = requireNotNull(database).openHelper.writableDatabase

        sqlite.query("SELECT title, viewCount FROM favorites WHERE videoUrl = 'video-a'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("", cursor.getString(0))
            assertEquals(0L, cursor.getLong(1))
        }
        sqlite.execSQL(
            "INSERT INTO library_mutation_outbox(" +
                "serverId, accountId, mutationKey, collection, kind, targetId, desiredPresent, " +
                "title, thumbnailUrl, durationSeconds, channelName, channelUrl, channelAvatarUrl, " +
                "viewCount, sessionGeneration, mutationVersion, state, createdAtMillis, " +
                "updatedAtMillis, attemptCount" +
                ") VALUES('instance-a', 'account-a', 'favorite:0:video-a', 'favorites', " +
                "'favorite', 'video-a', 1, 'Video', '', 1, '', '', '', 0, 4, 1, " +
                "'pending', 5, 5, 0)",
        )
        sqlite.execSQL(
            "INSERT INTO subscriptions(" +
                "serverId, accountId, channelUrl, name, avatarUrl, subscribedAtMillis" +
                ") VALUES('instance-a', 'account-a', 'channel-a', 'Channel', '', 6)",
        )
        sqlite.query("SELECT COUNT(*) FROM library_mutation_outbox").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
        }

        sqlite.execSQL("DELETE FROM accounts WHERE serverId = 'instance-a' AND accountId = 'account-a'")
        sqlite.query("SELECT COUNT(*) FROM library_mutation_outbox").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
        sqlite.query("SELECT COUNT(*) FROM subscriptions").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
    }

    private fun SupportSQLiteDatabase.insertVersion5Rows() {
        execSQL(
            "INSERT INTO servers(id, baseUrl, displayName, addedAt) " +
                "VALUES('instance-a', 'http://192.168.1.2:8080/api', 'Local TypeType', 1)",
        )
        execSQL(
            "INSERT INTO favorites(videoUrl, favoritedAtMillis) " +
                "VALUES('https://video.example/watch?v=1', 2)",
        )
    }

    private fun android.database.Cursor.hasColumn(name: String): Boolean {
        val nameIndex = getColumnIndexOrThrow("name")
        while (moveToNext()) {
            if (getString(nameIndex) == name) return true
        }
        return false
    }

    private companion object {
        const val DATABASE_NAME = "migration-test.db"
        const val FIRST_RETAINED_VERSION = 1
        const val CURRENT_VERSION = 20
        const val DRAFT_VERSION_10_SERVERS =
            "CREATE TABLE `servers` (" +
                "`id` TEXT NOT NULL, `baseUrl` TEXT NOT NULL, `displayName` TEXT NOT NULL, " +
                "`addedAt` INTEGER NOT NULL, `tagline` TEXT DEFAULT NULL, " +
                "`revision` TEXT NOT NULL DEFAULT '', `logoUrl` TEXT DEFAULT NULL, " +
                "`bannerUrl` TEXT DEFAULT NULL, `supportedServicesCsv` TEXT NOT NULL DEFAULT '', " +
                "`minAndroidClientVersion` TEXT DEFAULT NULL, " +
                "`youtubeRemoteLoginEnabled` INTEGER NOT NULL DEFAULT 0, " +
                "`youtubeRemoteLoginReady` INTEGER NOT NULL DEFAULT 0, " +
                "`youtubeRemoteLoginUnavailableReason` TEXT DEFAULT NULL, PRIMARY KEY(`id`))"
    }
}
