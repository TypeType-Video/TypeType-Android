package dev.typetype.android.feature.setup.welcome

sealed interface WelcomeEvent {
    data object NavigateToAddServer : WelcomeEvent
}
