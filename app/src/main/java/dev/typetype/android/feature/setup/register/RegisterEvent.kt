package dev.typetype.android.feature.setup.register

sealed interface RegisterEvent {
    data object NavigateToHome : RegisterEvent
    data object NavigateBack : RegisterEvent
    data class LaunchOidc(
        val authorizationUrl: String,
        val redirectScheme: String,
    ) : RegisterEvent
}
