package dev.typetype.android.core.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.typetype.android.data.recommendations.RemoteRecommenderFeedbackRepository
import dev.typetype.android.data.recommendations.RemoteRecommenderRepository
import dev.typetype.android.data.recommendations.RemoteRecommenderSettingsRepository
import dev.typetype.android.domain.recommendations.RecommenderFeedbackRepository
import dev.typetype.android.domain.recommendations.RecommenderRepository
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

    @Binds
    @Singleton
    abstract fun bindRecommenderFeedbackRepository(
        implementation: RemoteRecommenderFeedbackRepository,
    ): RecommenderFeedbackRepository

    @Binds
    @Singleton
    abstract fun bindRecommenderRepository(
        implementation: RemoteRecommenderRepository,
    ): RecommenderRepository
}
