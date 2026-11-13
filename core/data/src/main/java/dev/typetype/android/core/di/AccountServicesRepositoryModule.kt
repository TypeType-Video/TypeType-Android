package dev.typetype.android.core.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.typetype.android.data.allowlist.RemoteAllowedChannelsRepository
import dev.typetype.android.data.auth.RemoteAccountProfilesRepository
import dev.typetype.android.data.devicelink.RemoteDeviceLinkRepository
import dev.typetype.android.data.linkedsessions.RemoteLinkedSessionsRepository
import dev.typetype.android.data.subscriptions.RemoteSubscriptionGroupsRepository
import dev.typetype.android.domain.allowlist.AllowedChannelsRepository
import dev.typetype.android.domain.devicelink.DeviceLinkRepository
import dev.typetype.android.domain.linkedsessions.BiliBiliSessionRepository
import dev.typetype.android.domain.linkedsessions.YoutubeDeviceLoginRepository
import dev.typetype.android.domain.profiles.AccountProfilesRepository
import dev.typetype.android.domain.subscriptions.SubscriptionGroupsRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AccountServicesRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSubscriptionGroupsRepository(
        impl: RemoteSubscriptionGroupsRepository,
    ): SubscriptionGroupsRepository

    @Binds
    @Singleton
    abstract fun bindAccountProfilesRepository(impl: RemoteAccountProfilesRepository): AccountProfilesRepository

    @Binds
    @Singleton
    abstract fun bindYoutubeDeviceLoginRepository(impl: RemoteLinkedSessionsRepository): YoutubeDeviceLoginRepository

    @Binds
    @Singleton
    abstract fun bindBiliBiliSessionRepository(impl: RemoteLinkedSessionsRepository): BiliBiliSessionRepository

    @Binds
    @Singleton
    abstract fun bindAllowedChannelsRepository(impl: RemoteAllowedChannelsRepository): AllowedChannelsRepository

    @Binds
    @Singleton
    abstract fun bindDeviceLinkRepository(impl: RemoteDeviceLinkRepository): DeviceLinkRepository
}
