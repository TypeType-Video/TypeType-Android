package dev.typetype.android.feature.setup.welcome

sealed interface WelcomeAction {
    data object OnGetStartedClick : WelcomeAction
}
