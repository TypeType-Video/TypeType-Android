package dev.typetype.android.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.typetype.android.data.database.TypeTypeDatabase
import dev.typetype.android.data.database.TypeTypeDatabaseMigrations
import dev.typetype.android.data.account.AccountDao
import dev.typetype.android.data.download.DownloadDao
import dev.typetype.android.data.feed.FeedVideoDao
import dev.typetype.android.data.imports.YoutubeTakeoutImportDao
import dev.typetype.android.data.library.local.FavoritesDao
import dev.typetype.android.data.library.local.HistoryDao
import dev.typetype.android.data.library.local.PlaylistsDao
import dev.typetype.android.data.library.local.VideoMetaDao
import dev.typetype.android.data.library.local.WatchLaterDao
import dev.typetype.android.data.library.sync.ProgressOutboxDao
import dev.typetype.android.data.library.sync.LibrarySyncDao
import dev.typetype.android.data.library.sync.LibraryMutationDao
import dev.typetype.android.data.playback.PlaybackResumeDao
import dev.typetype.android.data.playback.PlaybackQueueDao
import dev.typetype.android.data.publicplaylist.SavedPublicPlaylistDao
import dev.typetype.android.data.server.ServerDao
import dev.typetype.android.data.subscriptions.SubscriptionDao
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "typetype_prefs")

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TypeTypeDatabase =
        Room.databaseBuilder(context, TypeTypeDatabase::class.java, "typetype.db")
            .addMigrations(*TypeTypeDatabaseMigrations.ALL)
            .fallbackToDestructiveMigration(false)
            .build()

    @Provides
    fun provideServerDao(database: TypeTypeDatabase): ServerDao = database.serverDao()

    @Provides
    fun provideFavoritesDao(database: TypeTypeDatabase): FavoritesDao = database.favoritesDao()

    @Provides
    fun provideHistoryDao(database: TypeTypeDatabase): HistoryDao = database.historyDao()

    @Provides
    fun provideWatchLaterDao(database: TypeTypeDatabase): WatchLaterDao = database.watchLaterDao()

    @Provides
    fun providePlaylistsDao(database: TypeTypeDatabase): PlaylistsDao = database.playlistsDao()

    @Provides
    fun provideVideoMetaDao(database: TypeTypeDatabase): VideoMetaDao = database.videoMetaDao()

    @Provides
    fun provideAccountDao(database: TypeTypeDatabase): AccountDao = database.accountDao()

    @Provides
    fun provideDownloadDao(database: TypeTypeDatabase): DownloadDao = database.downloadDao()

    @Provides
    fun provideProgressOutboxDao(database: TypeTypeDatabase): ProgressOutboxDao =
        database.progressOutboxDao()

    @Provides
    fun provideLibrarySyncDao(database: TypeTypeDatabase): LibrarySyncDao =
        database.librarySyncDao()

    @Provides
    fun provideLibraryMutationDao(database: TypeTypeDatabase): LibraryMutationDao =
        database.libraryMutationDao()

    @Provides
    fun provideSubscriptionDao(database: TypeTypeDatabase): SubscriptionDao =
        database.subscriptionDao()

    @Provides
    fun providePlaybackResumeDao(database: TypeTypeDatabase): PlaybackResumeDao =
        database.playbackResumeDao()

    @Provides
    fun providePlaybackQueueDao(database: TypeTypeDatabase): PlaybackQueueDao =
        database.playbackQueueDao()

    @Provides
    fun provideSavedPublicPlaylistDao(database: TypeTypeDatabase): SavedPublicPlaylistDao =
        database.savedPublicPlaylistDao()

    @Provides
    fun provideFeedVideoDao(database: TypeTypeDatabase): FeedVideoDao = database.feedVideoDao()

    @Provides
    fun provideYoutubeTakeoutImportDao(database: TypeTypeDatabase): YoutubeTakeoutImportDao =
        database.youtubeTakeoutImportDao()

    @Provides
    @Singleton
    fun providePreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.dataStore
}
