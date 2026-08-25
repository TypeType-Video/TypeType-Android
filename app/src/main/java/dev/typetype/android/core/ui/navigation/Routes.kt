package dev.typetype.android.core.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object WelcomeRoute

@Serializable
data object AddServerRoute

@Serializable
data class LoginRoute(
    val serverId: String,
    val accountId: String? = null,
)

@Serializable
data class RegisterRoute(val serverId: String)

@Serializable
data class ResetPasswordRoute(val serverId: String)

@Serializable
data object HomeRoute

@Serializable
data object ShortsRoute

@Serializable
data object SubscriptionsRoute

@Serializable
data object LibraryRoute

@Serializable
data class LibraryLandingRoute(val tab: String)

@Serializable
data object SettingsRoute

@Serializable
data object AccountsRoute

@Serializable
data object AppearanceRoute

@Serializable
data object ContentSettingsRoute

@Serializable
data object PlayerSettingsRoute

@Serializable
data object StorageSettingsRoute

@Serializable
data object ProfileSettingsRoute

@Serializable
data object ImportDataRoute

@Serializable
data object RssFeedsRoute

@Serializable
data object PrivacySettingsRoute

@Serializable
data object DiagnosticsRoute

@Serializable
data object BlockedSettingsRoute

@Serializable
data object AboutRoute

@Serializable
data class PlayerRoute(val videoUrl: String)

@Serializable
data object SearchRoute

@Serializable
data object NotificationsRoute

@Serializable
data class ChannelRoute(val channelUrl: String)

@Serializable
data class PlaylistRoute(val playlistId: String)

@Serializable
data class PublicPlaylistRoute(val playlistUrl: String)

@Serializable
data class PodcastRoute(val podcastUrl: String)
