package dev.typetype.android.feature.setup.register

sealed interface RegisterAction {
    data class OnNameChange(val value: String) : RegisterAction
    data class OnEmailChange(val value: String) : RegisterAction
    data class OnPasswordChange(val value: String) : RegisterAction
    data object OnRegisterClick : RegisterAction
    data object OnOidcClick : RegisterAction
    data class OnOidcCallback(val callbackUrl: String) : RegisterAction
    data object OnOidcBrowserUnavailable : RegisterAction
    data object OnOidcCancelled : RegisterAction
    data object OnRetryClick : RegisterAction
    data object OnBackClick : RegisterAction
}
