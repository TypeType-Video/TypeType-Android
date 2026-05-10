package dev.typetype.android.data.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
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
import dev.typetype.android.data.server.ServerDao
import dev.typetype.android.data.server.ServerEntity

@Database(
    entities = [
        ServerEntity::class,
        FavoriteEntity::class,
        HistoryEntity::class,
        WatchLaterEntity::class,
        PlaylistEntity::class,
        PlaylistVideoEntity::class,
        VideoMetaEntity::class,
    ],
    version = 5,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
    ],
)
abstract class TypeTypeDatabase : RoomDatabase() {
    abstract fun serverDao(): ServerDao
    abstract fun favoritesDao(): FavoritesDao
    abstract fun historyDao(): HistoryDao
    abstract fun watchLaterDao(): WatchLaterDao
    abstract fun playlistsDao(): PlaylistsDao
    abstract fun videoMetaDao(): VideoMetaDao
}
