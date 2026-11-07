package dev.typetype.android.core.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.typetype.android.data.usersettings.RemoteSeekbarIconRepository
import dev.typetype.android.data.usersettings.RemoteUserSettingsRepository
import dev.typetype.android.domain.usersettings.SeekbarIconRepository
import dev.typetype.android.domain.usersettings.UserSettingsRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class UserSettingsModule {
    @Binds
    @Singleton
    abstract fun bindUserSettingsRepository(impl: RemoteUserSettingsRepository): UserSettingsRepository

    @Binds
    @Singleton
    abstract fun bindSeekbarIconRepository(impl: RemoteSeekbarIconRepository): SeekbarIconRepository
}
