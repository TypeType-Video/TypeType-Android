package dev.typetype.android.core.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.typetype.android.data.actions.VideoActionsRepositoryImpl
import dev.typetype.android.data.account.RoomAccountRepository
import dev.typetype.android.data.auth.AuthRepositoryImpl
import dev.typetype.android.data.channel.ChannelRepositoryImpl
import dev.typetype.android.data.comments.CommentsRepositoryImpl
import dev.typetype.android.data.download.AndroidDownloadRepository
import dev.typetype.android.data.diagnostics.LocalDiagnosticsRepository
import dev.typetype.android.data.diagnostics.LocalCrashReportRepository
import dev.typetype.android.data.feed.HomeFeedRepositoryImpl
import dev.typetype.android.data.library.OfflineLibraryRepository
import dev.typetype.android.data.imports.RemoteImportRepository
import dev.typetype.android.data.library.RoomVideoMetaRepository
import dev.typetype.android.data.notifications.RemoteNotificationsRepository
import dev.typetype.android.data.preferences.DataStorePreferencesRepository
import dev.typetype.android.data.podcast.RemotePodcastRepository
import dev.typetype.android.data.playback.RoomPlaybackResumeRepository
import dev.typetype.android.data.playback.RoomPlaybackQueueRepository
import dev.typetype.android.data.profile.RemoteProfileRepository
import dev.typetype.android.data.publicplaylist.RemotePublicPlaylistRepository
import dev.typetype.android.data.publicplaylist.OfflineSavedPublicPlaylistRepository
import dev.typetype.android.data.search.SearchRepositoryImpl
import dev.typetype.android.data.searchhistory.RemoteSearchHistoryStore
import dev.typetype.android.data.server.RoomServerRepository
import dev.typetype.android.data.session.RemoteActiveSessionRepository
import dev.typetype.android.data.setup.SetupRepositoryImpl
import dev.typetype.android.data.stream.StreamRepositoryImpl
import dev.typetype.android.data.stream.SabrPlaybackRepositoryImpl
import dev.typetype.android.data.stream.RemoteSubtitleRepository
import dev.typetype.android.data.subscriptions.RemoteSubscriptionsRepository
import dev.typetype.android.data.support.RemoteSupportRepository
import dev.typetype.android.data.usersettings.RemoteUserSettingsRepository
import dev.typetype.android.domain.actions.VideoActionsRepository
import dev.typetype.android.domain.account.AccountRepository
import dev.typetype.android.domain.auth.AuthRepository
import dev.typetype.android.domain.channel.ChannelRepository
import dev.typetype.android.domain.comments.CommentsRepository
import dev.typetype.android.domain.download.DownloadRepository
import dev.typetype.android.domain.diagnostics.DiagnosticsRepository
import dev.typetype.android.domain.diagnostics.CrashReportRepository
import dev.typetype.android.domain.feed.HomeFeedRepository
import dev.typetype.android.domain.library.LibraryRepository
import dev.typetype.android.domain.imports.ImportRepository
import dev.typetype.android.domain.library.VideoMetaRepository
import dev.typetype.android.domain.notifications.NotificationsRepository
import dev.typetype.android.domain.preferences.PreferencesRepository
import dev.typetype.android.domain.podcast.PodcastRepository
import dev.typetype.android.domain.playback.PlaybackResumeRepository
import dev.typetype.android.domain.playback.PlaybackQueueRepository
import dev.typetype.android.domain.playback.PlaybackQueueController
import dev.typetype.android.domain.profile.ProfileRepository
import dev.typetype.android.domain.publicplaylist.PublicPlaylistRepository
import dev.typetype.android.domain.publicplaylist.SavedPublicPlaylistRepository
import dev.typetype.android.domain.search.SearchRepository
import dev.typetype.android.domain.searchhistory.SearchHistoryRepository
import dev.typetype.android.domain.server.ServerRepository
import dev.typetype.android.domain.session.ActiveSessionRepository
import dev.typetype.android.domain.setup.SetupRepository
import dev.typetype.android.domain.stream.StreamRepository
import dev.typetype.android.domain.stream.SabrPlaybackRepository
import dev.typetype.android.domain.stream.SubtitleRepository
import dev.typetype.android.domain.subscriptions.SubscriptionsRepository
import dev.typetype.android.domain.support.SupportRepository
import dev.typetype.android.domain.usersettings.UserSettingsRepository
import dev.typetype.android.services.PlaybackQueueCoordinator
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPlaybackQueueController(impl: PlaybackQueueCoordinator): PlaybackQueueController

    @Binds
    @Singleton
    abstract fun bindAccountRepository(impl: RoomAccountRepository): AccountRepository

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
    abstract fun bindPodcastRepository(impl: RemotePodcastRepository): PodcastRepository

    @Binds
    @Singleton
    abstract fun bindNotificationsRepository(
        impl: RemoteNotificationsRepository,
    ): NotificationsRepository

    @Binds
    @Singleton
    abstract fun bindStreamRepository(impl: StreamRepositoryImpl): StreamRepository

    @Binds
    @Singleton
    abstract fun bindSabrPlaybackRepository(impl: SabrPlaybackRepositoryImpl): SabrPlaybackRepository

    @Binds
    @Singleton
    abstract fun bindSubtitleRepository(impl: RemoteSubtitleRepository): SubtitleRepository

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
    abstract fun bindImportRepository(impl: RemoteImportRepository): ImportRepository

    @Binds
    @Singleton
    abstract fun bindVideoMetaRepository(impl: RoomVideoMetaRepository): VideoMetaRepository

    @Binds
    @Singleton
    abstract fun bindChannelRepository(impl: ChannelRepositoryImpl): ChannelRepository

    @Binds
    @Singleton
    abstract fun bindSearchHistoryRepository(impl: RemoteSearchHistoryStore): SearchHistoryRepository

    @Binds
    @Singleton
    abstract fun bindVideoActionsRepository(impl: VideoActionsRepositoryImpl): VideoActionsRepository

    @Binds
    @Singleton
    abstract fun bindUserSettingsRepository(impl: RemoteUserSettingsRepository): UserSettingsRepository

    @Binds
    @Singleton
    abstract fun bindProfileRepository(impl: RemoteProfileRepository): ProfileRepository

    @Binds
    @Singleton
    abstract fun bindPublicPlaylistRepository(
        impl: RemotePublicPlaylistRepository,
    ): PublicPlaylistRepository

    @Binds
    @Singleton
    abstract fun bindSavedPublicPlaylistRepository(
        impl: OfflineSavedPublicPlaylistRepository,
    ): SavedPublicPlaylistRepository

    @Binds
    @Singleton
    abstract fun bindSubscriptionsRepository(impl: RemoteSubscriptionsRepository): SubscriptionsRepository

    @Binds
    @Singleton
    abstract fun bindDownloadRepository(impl: AndroidDownloadRepository): DownloadRepository

    @Binds
    @Singleton
    abstract fun bindDiagnosticsRepository(impl: LocalDiagnosticsRepository): DiagnosticsRepository

    @Binds
    @Singleton
    abstract fun bindCrashReportRepository(impl: LocalCrashReportRepository): CrashReportRepository

    @Binds
    @Singleton
    abstract fun bindSupportRepository(impl: RemoteSupportRepository): SupportRepository

    @Binds
    @Singleton
    abstract fun bindActiveSessionRepository(
        impl: RemoteActiveSessionRepository,
    ): ActiveSessionRepository

    @Binds
    @Singleton
    abstract fun bindPlaybackResumeRepository(
        impl: RoomPlaybackResumeRepository,
    ): PlaybackResumeRepository

    @Binds
    @Singleton
    abstract fun bindPlaybackQueueRepository(
        impl: RoomPlaybackQueueRepository,
    ): PlaybackQueueRepository
}
