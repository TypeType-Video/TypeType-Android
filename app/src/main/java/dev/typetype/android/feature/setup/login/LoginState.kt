package dev.typetype.android.feature.setup.login

data class LoginState(
    val identifier: String = "",
    val password: String = "",
    val isSubmitting: Boolean = false,
    val isLoadingMethods: Boolean = true,
    val errorMessage: String? = null,
    val errorRequestId: String? = null,
    val instanceName: String = "",
    val localLoginEnabled: Boolean = false,
    val guestAllowed: Boolean = false,
    val registrationAllowed: Boolean = false,
    val oidcEnabled: Boolean = false,
    val oidcProviderName: String? = null,
    val oidcAutoRedirect: Boolean = false,
)
