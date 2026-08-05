package dev.typetype.android.feature.setup.register

data class RegisterState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val instanceName: String = "",
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val allowRegistration: Boolean = false,
    val bootstrapAvailable: Boolean = false,
    val localLoginEnabled: Boolean = false,
    val oidcEnabled: Boolean = false,
    val oidcProviderName: String? = null,
    val errorMessage: String? = null,
    val errorRequestId: String? = null,
) {
    val isRegistrationOpen: Boolean
        get() = localLoginEnabled && (allowRegistration || bootstrapAvailable)

    val isClosed: Boolean
        get() = !isLoading && !allowRegistration && !bootstrapAvailable
}
