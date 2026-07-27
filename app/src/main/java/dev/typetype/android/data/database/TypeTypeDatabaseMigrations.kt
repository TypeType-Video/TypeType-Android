package dev.typetype.android.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object TypeTypeDatabaseMigrations {
    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            TypeTypeDatabaseMigrationSql.dropUnscopedCaches(db)
            TypeTypeDatabaseMigrationSql.createScopedCaches(db)
        }
    }

    val MIGRATION_11_12: Migration = ServerSchemaRepairMigration

    val MIGRATION_12_13 = object : Migration(12, 13) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE `library_sync_state` (" +
                    "`serverId` TEXT NOT NULL, `accountId` TEXT NOT NULL, " +
                    "`collection` TEXT NOT NULL, `refreshGeneration` INTEGER NOT NULL, " +
                    "`lastAttemptAtMillis` INTEGER NOT NULL, " +
                    "`lastSuccessAtMillis` INTEGER, `lastFailureAtMillis` INTEGER, " +
                    "`failureCode` TEXT, `failureStatusCode` INTEGER, `requestId` TEXT, " +
                    "PRIMARY KEY(`serverId`, `accountId`, `collection`), " +
                    TypeTypeDatabaseMigrationSql.accountForeignKey() + ")",
            )
            db.execSQL(
                "CREATE INDEX `index_library_sync_state_serverId_accountId` " +
                    "ON `library_sync_state` (`serverId`, `accountId`)",
            )
        }
    }

    val MIGRATION_13_14 = object : Migration(13, 14) {
        override fun migrate(db: SupportSQLiteDatabase) {
            TypeTypeDatabaseMigrationSql.addCollectionMetadata(db)
            TypeTypeDatabaseMigrationSql.createSubscriptions(db)
            TypeTypeDatabaseMigrationSql.createLibraryMutationOutbox(db)
        }
    }

    val MIGRATION_14_15 = object : Migration(14, 15) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `playlists` ADD COLUMN `videoCount` INTEGER NOT NULL DEFAULT 0")
            db.execSQL(
                "UPDATE `playlists` SET `videoCount` = (" +
                    "SELECT COUNT(*) FROM `playlist_videos` " +
                    "WHERE `playlist_videos`.`playlistCacheKey` = `playlists`.`cacheKey`)",
            )
        }
    }

    val MIGRATION_16_17 = object : Migration(16, 17) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE `playback_queue` (" +
                    "`serverId` TEXT NOT NULL, `accountId` TEXT NOT NULL, " +
                    "`position` INTEGER NOT NULL, `currentIndex` INTEGER NOT NULL, " +
                    "`queueTitle` TEXT NOT NULL, `videoUrl` TEXT NOT NULL, " +
                    "`title` TEXT NOT NULL, `thumbnailUrl` TEXT NOT NULL, " +
                    "`durationSeconds` INTEGER NOT NULL, `channelName` TEXT NOT NULL, " +
                    "`updatedAtMillis` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`serverId`, `accountId`, `position`), " +
                    TypeTypeDatabaseMigrationSql.accountForeignKey() + ")",
            )
            db.execSQL(
                "CREATE INDEX `index_playback_queue_serverId_accountId` " +
                    "ON `playback_queue` (`serverId`, `accountId`)",
            )
        }
    }

    val MIGRATION_17_18 = object : Migration(17, 18) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE `playback_queue` " +
                    "ADD COLUMN `repeatMode` TEXT NOT NULL DEFAULT 'Off'",
            )
        }
    }

    val MIGRATION_18_19 = object : Migration(18, 19) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE `saved_public_playlists` (" +
                    "`serverId` TEXT NOT NULL, `accountId` TEXT NOT NULL, `id` TEXT NOT NULL, " +
                    "`publicPlaylistId` TEXT NOT NULL, `url` TEXT NOT NULL, `title` TEXT NOT NULL, " +
                    "`thumbnailUrl` TEXT NOT NULL, `uploaderName` TEXT NOT NULL, " +
                    "`streamCount` INTEGER NOT NULL, `playlistType` TEXT NOT NULL, " +
                    "`savedAtMillis` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`serverId`, `accountId`, `id`), " +
                    TypeTypeDatabaseMigrationSql.accountForeignKey() + ")",
            )
            db.execSQL(
                "CREATE INDEX `index_saved_public_playlists_serverId_accountId` " +
                    "ON `saved_public_playlists` (`serverId`, `accountId`)",
            )
            db.execSQL(
                "CREATE UNIQUE INDEX `index_saved_public_playlists_serverId_accountId_url` " +
                    "ON `saved_public_playlists` (`serverId`, `accountId`, `url`)",
            )
        }
    }

    val MIGRATION_19_20 = object : Migration(19, 20) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE `feed_videos` (" +
                    "`serverId` TEXT NOT NULL, `accountId` TEXT NOT NULL, `feed` TEXT NOT NULL, " +
                    "`position` INTEGER NOT NULL, `videoUrl` TEXT NOT NULL, `videoId` TEXT NOT NULL, " +
                    "`title` TEXT NOT NULL, `thumbnailUrl` TEXT NOT NULL, " +
                    "`uploaderName` TEXT NOT NULL, `uploaderUrl` TEXT NOT NULL, " +
                    "`uploaderAvatarUrl` TEXT NOT NULL, `uploaderVerified` INTEGER NOT NULL, " +
                    "`durationSeconds` INTEGER NOT NULL, `isLive` INTEGER NOT NULL, " +
                    "`viewCount` INTEGER NOT NULL, `uploadedAtMillis` INTEGER NOT NULL, " +
                    "`isShortFormContent` INTEGER NOT NULL, `shortDescription` TEXT, " +
                    "`publishedAtMillis` INTEGER, `isPostLive` INTEGER NOT NULL, " +
                    "`isLiveContent` INTEGER NOT NULL, `requiresMembership` INTEGER NOT NULL, " +
                    "`savedAtMillis` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`serverId`, `accountId`, `feed`, `videoUrl`), " +
                    TypeTypeDatabaseMigrationSql.accountForeignKey() + ")",
            )
            db.execSQL(
                "CREATE INDEX `index_feed_videos_serverId_accountId_feed` " +
                    "ON `feed_videos` (`serverId`, `accountId`, `feed`)",
            )
            db.execSQL(
                "CREATE UNIQUE INDEX `index_feed_videos_serverId_accountId_feed_position` " +
                    "ON `feed_videos` (`serverId`, `accountId`, `feed`, `position`)",
            )
        }
    }

    val ALL = arrayOf(
        MIGRATION_7_8,
        MIGRATION_11_12,
        MIGRATION_12_13,
        MIGRATION_13_14,
        MIGRATION_14_15,
        MIGRATION_16_17,
        MIGRATION_17_18,
        MIGRATION_18_19,
        MIGRATION_19_20,
    )
}
