package dev.typetype.android.core.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.typetype.android.data.server.RoomServerRepository
import dev.typetype.android.data.setup.SetupRepositoryImpl
import dev.typetype.android.domain.server.ServerRepository
import dev.typetype.android.domain.setup.SetupRepository
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
}
