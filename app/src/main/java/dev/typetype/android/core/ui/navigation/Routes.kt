package dev.typetype.android.core.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object WelcomeRoute

@Serializable
data object AddServerRoute

@Serializable
data class LoginRoute(val serverId: String)

@Serializable
data object HomeRoute
