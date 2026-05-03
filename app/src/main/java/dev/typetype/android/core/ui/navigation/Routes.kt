package dev.typetype.android.core.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object WelcomeRoute

@Serializable
data object AddServerRoute

@Serializable
data class LoginRoute(
    val serverId: String,
    val guestAllowed: Boolean,
    val registrationAllowed: Boolean,
)

@Serializable
data object HomeRoute

@Serializable
data object SubscriptionsRoute

@Serializable
data object LibraryRoute

@Serializable
data object SettingsRoute

@Serializable
data object AppearanceRoute

@Serializable
data class PlayerRoute(val videoUrl: String)
