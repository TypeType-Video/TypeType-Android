package dev.typetype.android.core.ui.error

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.typetype.android.R
import dev.typetype.android.core.error.CodedFailure
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserErrorMapper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun message(failure: Throwable?, fallbackRes: Int): String =
        context.getString(resourceFor(classifyUserError(failure), fallbackRes))

    fun details(failure: Throwable?, fallbackRes: Int): UserErrorDetails =
        UserErrorDetails(
            message = message(failure, fallbackRes),
            requestId = (failure as? CodedFailure)?.requestId?.takeIf(SAFE_REQUEST_ID::matches),
        )

    fun message(
        failureCode: String?,
        statusCode: Int?,
        fallbackRes: Int,
    ): String = context.getString(
        resourceFor(classifyUserError(failureCode, statusCode), fallbackRes),
    )

    fun authenticationMessage(
        failure: Throwable?,
        fallbackRes: Int,
        rejectedRes: Int = fallbackRes,
    ): String {
        val kind = classifyUserError(failure)
        val resource = if (kind == UserErrorKind.SignInAgain || kind == UserErrorKind.PermissionDenied) {
            rejectedRes
        } else {
            resourceFor(kind, fallbackRes)
        }
        return context.getString(resource)
    }

    fun authenticationDetails(
        failure: Throwable?,
        fallbackRes: Int,
        rejectedRes: Int = fallbackRes,
    ): UserErrorDetails = UserErrorDetails(
        message = authenticationMessage(failure, fallbackRes, rejectedRes),
        requestId = (failure as? CodedFailure)?.requestId?.takeIf(SAFE_REQUEST_ID::matches),
    )

    private fun resourceFor(kind: UserErrorKind, fallbackRes: Int): Int =
        when (kind) {
            UserErrorKind.SecureConnectionFailed -> R.string.error_secure_connection_failed
            UserErrorKind.NetworkUnavailable -> R.string.error_network_unavailable
            UserErrorKind.SignInAgain -> R.string.error_sign_in_again
            UserErrorKind.PermissionDenied -> R.string.error_permission_denied
            UserErrorKind.ContentUnavailable -> R.string.error_content_unavailable
            UserErrorKind.Conflict -> R.string.error_conflict
            UserErrorKind.RateLimited -> R.string.error_rate_limited
            UserErrorKind.ServerUnavailable -> R.string.error_server_unavailable
            UserErrorKind.InstanceUnavailable -> R.string.error_instance_unavailable
            UserErrorKind.InstanceIncompatible -> R.string.error_instance_incompatible
            UserErrorKind.Fallback -> fallbackRes
        }
}

data class UserErrorDetails(
    val message: String,
    val requestId: String?,
)

private val SAFE_REQUEST_ID = Regex("[A-Za-z0-9._:-]{1,128}")
