package dev.typetype.android.data.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import dev.typetype.android.data.account.AccountDao
import dev.typetype.android.data.account.AccountEntity
import dev.typetype.android.data.download.DownloadDao
import dev.typetype.android.data.download.DownloadEntity
import dev.typetype.android.data.feed.FeedVideoDao
import dev.typetype.android.data.feed.FeedVideoEntity
import dev.typetype.android.data.imports.YoutubeTakeoutImportDao
import dev.typetype.android.data.imports.YoutubeTakeoutImportEntity
import dev.typetype.android.data.library.local.FavoriteEntity
import dev.typetype.android.data.library.local.FavoritesDao
import dev.typetype.android.data.library.local.HistoryDao
import dev.typetype.android.data.library.local.HistoryEntity
import dev.typetype.android.data.library.local.PlaylistEntity
import dev.typetype.android.data.library.local.PlaylistVideoEntity
import dev.typetype.android.data.library.local.PlaylistsDao
import dev.typetype.android.data.library.local.VideoMetaDao
import dev.typetype.android.data.library.local.VideoMetaEntity
import dev.typetype.android.data.library.local.WatchLaterDao
import dev.typetype.android.data.library.local.WatchLaterEntity
import dev.typetype.android.data.library.sync.ProgressOutboxDao
import dev.typetype.android.data.library.sync.ProgressOutboxEntity
import dev.typetype.android.data.library.sync.LibrarySyncDao
import dev.typetype.android.data.library.sync.LibrarySyncEntity
import dev.typetype.android.data.library.sync.LibraryMutationDao
import dev.typetype.android.data.library.sync.LibraryMutationEntity
import dev.typetype.android.data.playback.PlaybackResumeDao
import dev.typetype.android.data.playback.PlaybackResumeEntity
import dev.typetype.android.data.playback.PlaybackQueueDao
import dev.typetype.android.data.playback.PlaybackQueueEntity
import dev.typetype.android.data.publicplaylist.SavedPublicPlaylistDao
import dev.typetype.android.data.publicplaylist.SavedPublicPlaylistEntity
import dev.typetype.android.data.server.ServerDao
import dev.typetype.android.data.server.ServerEntity
import dev.typetype.android.data.subscriptions.SubscriptionDao
import dev.typetype.android.data.subscriptions.SubscriptionEntity

@Database(
    entities = [
        ServerEntity::class,
        FavoriteEntity::class,
        HistoryEntity::class,
        WatchLaterEntity::class,
        PlaylistEntity::class,
        PlaylistVideoEntity::class,
        VideoMetaEntity::class,
        AccountEntity::class,
        DownloadEntity::class,
        ProgressOutboxEntity::class,
        LibrarySyncEntity::class,
        LibraryMutationEntity::class,
        SubscriptionEntity::class,
        PlaybackResumeEntity::class,
        PlaybackQueueEntity::class,
        SavedPublicPlaylistEntity::class,
        FeedVideoEntity::class,
        YoutubeTakeoutImportEntity::class,
    ],
    version = 22,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6),
        AutoMigration(from = 6, to = 7),
        AutoMigration(from = 8, to = 9),
        AutoMigration(from = 9, to = 10),
        AutoMigration(from = 10, to = 11),
        AutoMigration(from = 15, to = 16),
    ],
)
abstract class TypeTypeDatabase : RoomDatabase() {
    abstract fun serverDao(): ServerDao
    abstract fun favoritesDao(): FavoritesDao
    abstract fun historyDao(): HistoryDao
    abstract fun watchLaterDao(): WatchLaterDao
    abstract fun playlistsDao(): PlaylistsDao
    abstract fun videoMetaDao(): VideoMetaDao
    abstract fun accountDao(): AccountDao
    abstract fun downloadDao(): DownloadDao
    abstract fun progressOutboxDao(): ProgressOutboxDao
    abstract fun librarySyncDao(): LibrarySyncDao
    abstract fun libraryMutationDao(): LibraryMutationDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun playbackResumeDao(): PlaybackResumeDao
    abstract fun playbackQueueDao(): PlaybackQueueDao
    abstract fun savedPublicPlaylistDao(): SavedPublicPlaylistDao
    abstract fun feedVideoDao(): FeedVideoDao
    abstract fun youtubeTakeoutImportDao(): YoutubeTakeoutImportDao
}
