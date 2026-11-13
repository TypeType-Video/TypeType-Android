package dev.typetype.android.core.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.typetype.android.data.channel.ChannelPostsRepositoryImpl
import dev.typetype.android.data.livechat.RemoteLiveChatRepository
import dev.typetype.android.data.progress.WatchProgressCache
import dev.typetype.android.domain.channel.ChannelPostsRepository
import dev.typetype.android.domain.livechat.LiveChatRepository
import dev.typetype.android.domain.progress.WatchProgressSource
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LiveContentRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindWatchProgressSource(impl: WatchProgressCache): WatchProgressSource

    @Binds
    @Singleton
    abstract fun bindLiveChatRepository(impl: RemoteLiveChatRepository): LiveChatRepository

    @Binds
    @Singleton
    abstract fun bindChannelPostsRepository(impl: ChannelPostsRepositoryImpl): ChannelPostsRepository
}
