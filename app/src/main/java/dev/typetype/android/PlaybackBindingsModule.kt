package dev.typetype.android

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.typetype.android.core.ui.prewarm.PlaybackPrewarm
import dev.typetype.android.domain.playback.PlaybackQueueController
import dev.typetype.android.feature.player.PlaybackPrewarmLauncher
import dev.typetype.android.services.PlaybackQueueCoordinator
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class PlaybackBindingsModule {
    @Binds
    @Singleton
    abstract fun bindPlaybackQueueController(impl: PlaybackQueueCoordinator): PlaybackQueueController

    @Binds
    @Singleton
    abstract fun bindPlaybackPrewarm(impl: PlaybackPrewarmLauncher): PlaybackPrewarm
}
