package dev.typetype.android.core.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.typetype.android.data.appupdate.AppUpdateDownloader
import dev.typetype.android.data.appupdate.DataStoreAppUpdateSettingsRepository
import dev.typetype.android.data.appupdate.GitHubAppUpdateRepository
import dev.typetype.android.domain.appupdate.AppUpdateFileDownloader
import dev.typetype.android.domain.appupdate.AppUpdateRepository
import dev.typetype.android.domain.appupdate.AppUpdateSettingsRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppUpdateModule {
    @Binds
    @Singleton
    abstract fun bindAppUpdateFileDownloader(impl: AppUpdateDownloader): AppUpdateFileDownloader

    @Binds
    @Singleton
    abstract fun bindAppUpdateRepository(impl: GitHubAppUpdateRepository): AppUpdateRepository

    @Binds
    @Singleton
    abstract fun bindAppUpdateSettingsRepository(
        impl: DataStoreAppUpdateSettingsRepository,
    ): AppUpdateSettingsRepository
}
