package dev.typetype.android.feature.setup.resetpassword

data class ResetPasswordState(
    val resetToken: String = "",
    val newPassword: String = "",
    val isSubmitting: Boolean = false,
    val isComplete: Boolean = false,
    val errorKey: String? = null,
    val errorRequestId: String? = null,
) {
    val canSubmit: Boolean
        get() = resetToken.isNotBlank() && newPassword.isNotBlank() && !isSubmitting
}
