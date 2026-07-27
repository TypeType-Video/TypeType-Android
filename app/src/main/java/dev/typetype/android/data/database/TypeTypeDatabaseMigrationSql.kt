package dev.typetype.android.data.database

import androidx.sqlite.db.SupportSQLiteDatabase

internal object TypeTypeDatabaseMigrationSql {
    fun addCollectionMetadata(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `favorites` ADD COLUMN `title` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `favorites` ADD COLUMN `thumbnailUrl` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `favorites` ADD COLUMN `durationSeconds` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `favorites` ADD COLUMN `channelName` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `favorites` ADD COLUMN `channelUrl` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `favorites` ADD COLUMN `channelAvatarUrl` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `favorites` ADD COLUMN `viewCount` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `watch_later` ADD COLUMN `channelName` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `watch_later` ADD COLUMN `channelUrl` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `watch_later` ADD COLUMN `channelAvatarUrl` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `watch_later` ADD COLUMN `viewCount` INTEGER NOT NULL DEFAULT 0")
    }

    fun createSubscriptions(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE `subscriptions` (" +
                "`serverId` TEXT NOT NULL, `accountId` TEXT NOT NULL, `channelUrl` TEXT NOT NULL, " +
                "`name` TEXT NOT NULL, `avatarUrl` TEXT NOT NULL, `subscribedAtMillis` INTEGER NOT NULL, " +
                "PRIMARY KEY(`serverId`, `accountId`, `channelUrl`), " + accountForeignKey() + ")",
        )
        db.execSQL(
            "CREATE INDEX `index_subscriptions_serverId_accountId` " +
                "ON `subscriptions` (`serverId`, `accountId`)",
        )
    }

    fun createLibraryMutationOutbox(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE `library_mutation_outbox` (" +
                "`serverId` TEXT NOT NULL, `accountId` TEXT NOT NULL, `mutationKey` TEXT NOT NULL, " +
                "`collection` TEXT NOT NULL, `kind` TEXT NOT NULL, `targetId` TEXT NOT NULL, " +
                "`parentId` TEXT, `desiredPresent` INTEGER NOT NULL, `title` TEXT NOT NULL, " +
                "`thumbnailUrl` TEXT NOT NULL, `durationSeconds` INTEGER NOT NULL, " +
                "`channelName` TEXT NOT NULL, `channelUrl` TEXT NOT NULL, " +
                "`channelAvatarUrl` TEXT NOT NULL, `viewCount` INTEGER NOT NULL, " +
                "`sessionGeneration` INTEGER NOT NULL, `mutationVersion` INTEGER NOT NULL, " +
                "`state` TEXT NOT NULL, `createdAtMillis` INTEGER NOT NULL, " +
                "`updatedAtMillis` INTEGER NOT NULL, `lastAttemptAtMillis` INTEGER, " +
                "`attemptCount` INTEGER NOT NULL, `failureCode` TEXT, " +
                "`failureStatusCode` INTEGER, `requestId` TEXT, " +
                "PRIMARY KEY(`serverId`, `accountId`, `mutationKey`), " + accountForeignKey() + ")",
        )
        db.execSQL(
            "CREATE INDEX `index_library_mutation_outbox_serverId_accountId_sessionGeneration_state` " +
                "ON `library_mutation_outbox` (`serverId`, `accountId`, `sessionGeneration`, `state`)",
        )
        db.execSQL(
            "CREATE INDEX `index_library_mutation_outbox_serverId_accountId_collection` " +
                "ON `library_mutation_outbox` (`serverId`, `accountId`, `collection`)",
        )
    }

    fun dropUnscopedCaches(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS `playlist_videos`")
        db.execSQL("DROP TABLE IF EXISTS `playlists`")
        db.execSQL("DROP TABLE IF EXISTS `video_meta`")
        db.execSQL("DROP TABLE IF EXISTS `watch_later`")
        db.execSQL("DROP TABLE IF EXISTS `history`")
        db.execSQL("DROP TABLE IF EXISTS `favorites`")
    }

    fun createScopedCaches(db: SupportSQLiteDatabase) {
        createFavorites(db)
        createHistory(db)
        createWatchLater(db)
        createPlaylists(db)
        createVideoMeta(db)
    }

    private fun createFavorites(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE `favorites` (" +
                "`serverId` TEXT NOT NULL, `accountId` TEXT NOT NULL, " +
                "`videoUrl` TEXT NOT NULL, `favoritedAtMillis` INTEGER NOT NULL, " +
                "PRIMARY KEY(`serverId`, `accountId`, `videoUrl`), " +
                accountForeignKey() + ")",
        )
        db.execSQL(
            "CREATE INDEX `index_favorites_serverId_accountId` " +
                "ON `favorites` (`serverId`, `accountId`)",
        )
    }

    private fun createHistory(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE `history` (" +
                "`serverId` TEXT NOT NULL, `accountId` TEXT NOT NULL, `id` TEXT NOT NULL, " +
                "`url` TEXT NOT NULL, `title` TEXT NOT NULL, `thumbnailUrl` TEXT NOT NULL, " +
                "`channelName` TEXT NOT NULL, `channelUrl` TEXT NOT NULL DEFAULT '', " +
                "`channelAvatarUrl` TEXT NOT NULL DEFAULT '', `durationSeconds` INTEGER NOT NULL, " +
                "`progressSeconds` INTEGER NOT NULL, `watchedAtMillis` INTEGER NOT NULL, " +
                "PRIMARY KEY(`serverId`, `accountId`, `id`), " +
                accountForeignKey() + ")",
        )
        db.execSQL(
            "CREATE INDEX `index_history_serverId_accountId` " +
                "ON `history` (`serverId`, `accountId`)",
        )
        db.execSQL(
            "CREATE INDEX `index_history_serverId_accountId_url` " +
                "ON `history` (`serverId`, `accountId`, `url`)",
        )
    }

    private fun createWatchLater(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE `watch_later` (" +
                "`serverId` TEXT NOT NULL, `accountId` TEXT NOT NULL, `url` TEXT NOT NULL, " +
                "`title` TEXT NOT NULL, `thumbnailUrl` TEXT NOT NULL, " +
                "`durationSeconds` INTEGER NOT NULL, `addedAtMillis` INTEGER NOT NULL, " +
                "PRIMARY KEY(`serverId`, `accountId`, `url`), " +
                accountForeignKey() + ")",
        )
        db.execSQL(
            "CREATE INDEX `index_watch_later_serverId_accountId` " +
                "ON `watch_later` (`serverId`, `accountId`)",
        )
    }

    private fun createPlaylists(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE `playlists` (" +
                "`cacheKey` TEXT NOT NULL, `serverId` TEXT NOT NULL, `accountId` TEXT NOT NULL, " +
                "`id` TEXT NOT NULL, `name` TEXT NOT NULL, `description` TEXT NOT NULL, " +
                "`createdAtMillis` INTEGER NOT NULL, PRIMARY KEY(`cacheKey`), " +
                accountForeignKey() + ")",
        )
        db.execSQL(
            "CREATE INDEX `index_playlists_serverId_accountId` " +
                "ON `playlists` (`serverId`, `accountId`)",
        )
        db.execSQL(
            "CREATE UNIQUE INDEX `index_playlists_serverId_accountId_id` " +
                "ON `playlists` (`serverId`, `accountId`, `id`)",
        )
        db.execSQL(
            "CREATE TABLE `playlist_videos` (" +
                "`playlistCacheKey` TEXT NOT NULL, `playlistId` TEXT NOT NULL, `id` TEXT NOT NULL, " +
                "`url` TEXT NOT NULL, `title` TEXT NOT NULL, `thumbnailUrl` TEXT NOT NULL, " +
                "`durationSeconds` INTEGER NOT NULL, `position` INTEGER NOT NULL, " +
                "`channelName` TEXT NOT NULL DEFAULT '', `channelUrl` TEXT NOT NULL DEFAULT '', " +
                "`channelAvatarUrl` TEXT NOT NULL DEFAULT '', `viewCount` INTEGER NOT NULL DEFAULT 0, " +
                "PRIMARY KEY(`playlistCacheKey`, `id`), " +
                "FOREIGN KEY(`playlistCacheKey`) REFERENCES `playlists`(`cacheKey`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE)",
        )
        db.execSQL(
            "CREATE INDEX `index_playlist_videos_playlistCacheKey` " +
                "ON `playlist_videos` (`playlistCacheKey`)",
        )
    }

    private fun createVideoMeta(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE `video_meta` (" +
                "`serverId` TEXT NOT NULL, `accountId` TEXT NOT NULL, `videoUrl` TEXT NOT NULL, " +
                "`channelName` TEXT NOT NULL, `channelUrl` TEXT NOT NULL, " +
                "`channelAvatarUrl` TEXT NOT NULL, `viewCount` INTEGER NOT NULL, " +
                "`updatedAtMillis` INTEGER NOT NULL, " +
                "PRIMARY KEY(`serverId`, `accountId`, `videoUrl`), " +
                accountForeignKey() + ")",
        )
        db.execSQL(
            "CREATE INDEX `index_video_meta_serverId_accountId` " +
                "ON `video_meta` (`serverId`, `accountId`)",
        )
    }

    fun accountForeignKey(): String =
        "FOREIGN KEY(`serverId`, `accountId`) REFERENCES `accounts`(`serverId`, `accountId`) " +
            "ON UPDATE NO ACTION ON DELETE CASCADE"
}
