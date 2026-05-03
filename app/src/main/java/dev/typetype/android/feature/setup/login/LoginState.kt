package dev.typetype.android.feature.setup.login

data class LoginState(
    val identifier: String = "",
    val password: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val guestAllowed: Boolean = false,
    val registrationAllowed: Boolean = false,
)
