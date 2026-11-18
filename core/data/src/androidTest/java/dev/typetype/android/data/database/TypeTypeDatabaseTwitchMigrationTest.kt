package dev.typetype.android.data.database

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
class TypeTypeDatabaseTwitchMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        instrumentation = InstrumentationRegistry.getInstrumentation(),
        databaseClass = TypeTypeDatabase::class.java,
    )

    @After
    fun deleteDatabase() {
        ApplicationProvider.getApplicationContext<android.content.Context>().deleteDatabase(DATABASE_NAME)
    }

    @Test
    fun migratesVersion24WithTwitchUnavailable() {
        helper.createDatabase(DATABASE_NAME, 24).apply {
            execSQL(
                "INSERT INTO servers(id, baseUrl, displayName, addedAt) " +
                    "VALUES('instance-a', 'https://example.test/api', 'TypeType', 1)",
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            DATABASE_NAME,
            25,
            true,
            TypeTypeDatabaseMigrations.MIGRATION_24_25,
        )

        migrated.query("SELECT displayName, twitchEnabled FROM servers WHERE id = 'instance-a'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("TypeType", cursor.getString(0))
            assertEquals(0, cursor.getInt(1))
        }
        migrated.close()
    }

    private companion object {
        const val DATABASE_NAME = "twitch-migration-test.db"
    }
}
