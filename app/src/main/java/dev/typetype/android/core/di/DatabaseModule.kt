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
import dev.typetype.android.data.library.local.FavoritesDao
import dev.typetype.android.data.library.local.HistoryDao
import dev.typetype.android.data.library.local.PlaylistsDao
import dev.typetype.android.data.library.local.VideoMetaDao
import dev.typetype.android.data.library.local.WatchLaterDao
import dev.typetype.android.data.server.ServerDao
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "typetype_prefs")

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TypeTypeDatabase =
        Room.databaseBuilder(context, TypeTypeDatabase::class.java, "typetype.db")
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
    @Singleton
    fun providePreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.dataStore
}
