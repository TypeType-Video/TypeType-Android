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
class PlaylistSummaryMigrationTest {
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
    fun migrationBackfillsPlaylistVideoCountWithoutDroppingChildren() {
        helper.createDatabase(DATABASE_NAME, 14).apply {
            execSQL(
                "INSERT INTO servers(id, baseUrl, displayName, addedAt) " +
                    "VALUES('server', 'https://example.test/api/', 'TypeType', 1)",
            )
            execSQL(
                "INSERT INTO accounts(serverId, accountId, isGuest, lastUsedAt, sessionGeneration) " +
                    "VALUES('server', 'account', 0, 1, 1)",
            )
            execSQL(
                "INSERT INTO playlists(cacheKey, serverId, accountId, id, name, description, createdAtMillis) " +
                    "VALUES('key', 'server', 'account', 'playlist', 'Saved', '', 1)",
            )
            repeat(2) { index ->
                execSQL(
                    "INSERT INTO playlist_videos(" +
                        "playlistCacheKey, playlistId, id, url, title, thumbnailUrl, " +
                        "durationSeconds, position) VALUES(" +
                        "'key', 'playlist', 'video-$index', 'url-$index', 'Video', '', 10, $index)",
                )
            }
            close()
        }

        database = Room.databaseBuilder(context, TypeTypeDatabase::class.java, DATABASE_NAME)
            .addMigrations(*TypeTypeDatabaseMigrations.ALL)
            .build()
        val sqlite = requireNotNull(database).openHelper.writableDatabase

        sqlite.query("SELECT videoCount FROM playlists WHERE id = 'playlist'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(2, cursor.getInt(0))
        }
        sqlite.query("SELECT COUNT(*) FROM playlist_videos").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(2, cursor.getInt(0))
        }
    }

    private companion object {
        const val DATABASE_NAME = "playlist-summary-migration.db"
    }
}
