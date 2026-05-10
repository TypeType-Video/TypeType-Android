package dev.typetype.android.core.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.typetype.android.data.actions.VideoActionsRepositoryImpl
import dev.typetype.android.data.auth.AuthRepositoryImpl
import dev.typetype.android.data.channel.ChannelRepositoryImpl
import dev.typetype.android.data.comments.CommentsRepositoryImpl
import dev.typetype.android.data.feed.HomeFeedRepositoryImpl
import dev.typetype.android.data.library.OfflineLibraryRepository
import dev.typetype.android.data.preferences.DataStorePreferencesRepository
import dev.typetype.android.data.search.SearchRepositoryImpl
import dev.typetype.android.data.searchhistory.RemoteSearchHistoryStore
import dev.typetype.android.data.server.RoomServerRepository
import dev.typetype.android.data.setup.SetupRepositoryImpl
import dev.typetype.android.data.stream.StreamRepositoryImpl
import dev.typetype.android.domain.actions.VideoActionsRepository
import dev.typetype.android.domain.auth.AuthRepository
import dev.typetype.android.domain.channel.ChannelRepository
import dev.typetype.android.domain.comments.CommentsRepository
import dev.typetype.android.domain.feed.HomeFeedRepository
import dev.typetype.android.domain.library.LibraryRepository
import dev.typetype.android.domain.preferences.PreferencesRepository
import dev.typetype.android.domain.search.SearchRepository
import dev.typetype.android.domain.searchhistory.SearchHistoryRepository
import dev.typetype.android.domain.server.ServerRepository
import dev.typetype.android.domain.setup.SetupRepository
import dev.typetype.android.domain.stream.StreamRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindServerRepository(impl: RoomServerRepository): ServerRepository

    @Binds
    @Singleton
    abstract fun bindSetupRepository(impl: SetupRepositoryImpl): SetupRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindHomeFeedRepository(impl: HomeFeedRepositoryImpl): HomeFeedRepository

    @Binds
    @Singleton
    abstract fun bindPreferencesRepository(impl: DataStorePreferencesRepository): PreferencesRepository

    @Binds
    @Singleton
    abstract fun bindStreamRepository(impl: StreamRepositoryImpl): StreamRepository

    @Binds
    @Singleton
    abstract fun bindCommentsRepository(impl: CommentsRepositoryImpl): CommentsRepository

    @Binds
    @Singleton
    abstract fun bindSearchRepository(impl: SearchRepositoryImpl): SearchRepository

    @Binds
    @Singleton
    abstract fun bindLibraryRepository(impl: OfflineLibraryRepository): LibraryRepository

    @Binds
    @Singleton
    abstract fun bindChannelRepository(impl: ChannelRepositoryImpl): ChannelRepository

    @Binds
    @Singleton
    abstract fun bindSearchHistoryRepository(impl: RemoteSearchHistoryStore): SearchHistoryRepository

    @Binds
    @Singleton
    abstract fun bindVideoActionsRepository(impl: VideoActionsRepositoryImpl): VideoActionsRepository
}
