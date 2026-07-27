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
class FeedCacheMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        instrumentation = InstrumentationRegistry.getInstrumentation(),
        databaseClass = TypeTypeDatabase::class.java,
    )

    @Test
    fun migration19To20CreatesScopedFeedCacheWithCascade() {
        helper.createDatabase(DATABASE_NAME, 19).apply {
            execSQL(
                "INSERT INTO servers(id, baseUrl, displayName, addedAt) " +
                    "VALUES('instance-a', 'https://example.test/api', 'TypeType', 1)",
            )
            execSQL(
                "INSERT INTO accounts(serverId, accountId, isGuest, lastUsedAt, sessionGeneration) " +
                    "VALUES('instance-a', 'account-a', 0, 2, 1)",
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            DATABASE_NAME,
            20,
            true,
            TypeTypeDatabaseMigrations.MIGRATION_19_20,
        )
        migrated.execSQL("PRAGMA foreign_keys = ON")
        migrated.execSQL(
            "INSERT INTO feed_videos(" +
                "serverId, accountId, feed, position, videoUrl, videoId, title, thumbnailUrl, " +
                "uploaderName, uploaderUrl, uploaderAvatarUrl, uploaderVerified, durationSeconds, " +
                "isLive, viewCount, uploadedAtMillis, isShortFormContent, shortDescription, " +
                "publishedAtMillis, isPostLive, isLiveContent, requiresMembership, savedAtMillis" +
                ") VALUES('instance-a', 'account-a', 'home', 0, 'video-url', 'video-id', " +
                "'Title', '', 'Channel', '', '', 0, 10, 0, 20, 30, 0, NULL, NULL, 0, 0, 0, 40)",
        )
        migrated.query("SELECT title FROM feed_videos").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Title", cursor.getString(0))
        }

        migrated.execSQL("DELETE FROM accounts WHERE serverId = 'instance-a' AND accountId = 'account-a'")
        migrated.query("SELECT COUNT(*) FROM feed_videos").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
        migrated.close()
    }

    private companion object {
        const val DATABASE_NAME = "feed-cache-migration.db"
    }
}
