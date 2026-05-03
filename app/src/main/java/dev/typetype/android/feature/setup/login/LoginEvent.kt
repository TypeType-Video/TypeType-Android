package dev.typetype.android.feature.setup.login

sealed interface LoginEvent {
    data object NavigateToHome : LoginEvent
    data object NavigateBack : LoginEvent
}
