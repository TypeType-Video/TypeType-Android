package dev.typetype.android.core.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.typetype.android.data.twitch.RemoteTwitchAccountRepository
import dev.typetype.android.data.twitch.RemoteTwitchRepository
import dev.typetype.android.domain.twitch.TwitchAccountRepository
import dev.typetype.android.domain.twitch.TwitchRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TwitchModule {
    @Binds
    @Singleton
    abstract fun bindTwitchRepository(impl: RemoteTwitchRepository): TwitchRepository

    @Binds
    @Singleton
    abstract fun bindTwitchAccountRepository(impl: RemoteTwitchAccountRepository): TwitchAccountRepository
}
