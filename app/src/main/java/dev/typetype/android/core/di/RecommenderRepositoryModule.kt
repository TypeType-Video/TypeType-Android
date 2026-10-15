package dev.typetype.android.core.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.typetype.android.data.recommendations.RemoteRecommenderSettingsRepository
import dev.typetype.android.domain.recommendations.RecommenderSettingsRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RecommenderRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindRecommenderSettingsRepository(
        implementation: RemoteRecommenderSettingsRepository,
    ): RecommenderSettingsRepository
}
