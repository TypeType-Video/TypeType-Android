package dev.typetype.android.feature.setup.login

sealed interface LoginAction {
    data class OnIdentifierChange(val value: String) : LoginAction
    data class OnPasswordChange(val value: String) : LoginAction
    data object OnLoginClick : LoginAction
    data object OnOidcClick : LoginAction
    data class OnOidcCallback(val callbackUrl: String) : LoginAction
    data object OnOidcBrowserUnavailable : LoginAction
    data object OnOidcCancelled : LoginAction
    data object OnContinueAsGuestClick : LoginAction
    data object OnBackClick : LoginAction
}
