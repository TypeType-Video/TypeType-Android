package dev.typetype.android.feature.setup.login

sealed interface LoginEvent {
    data object NavigateToHome : LoginEvent
    data object NavigateBack : LoginEvent
    data class NavigateToResetPassword(val serverId: String) : LoginEvent
    data class LaunchOidc(
        val authorizationUrl: String,
        val redirectScheme: String,
    ) : LoginEvent
}
