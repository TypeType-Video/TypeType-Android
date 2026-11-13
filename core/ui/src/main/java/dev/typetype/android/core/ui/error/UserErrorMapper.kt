package dev.typetype.android.core.ui.error

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.typetype.android.R
import dev.typetype.android.core.error.CodedFailure
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserErrorMapper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun message(failure: Throwable?, fallbackRes: Int, missingRes: Int? = null): String =
        context.getString(resourceFor(classifyUserError(failure, isOnline()), fallbackRes, missingRes))

    fun details(failure: Throwable?, fallbackRes: Int, missingRes: Int? = null): UserErrorDetails =
        UserErrorDetails(
            message = message(failure, fallbackRes, missingRes),
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
        val kind = classifyUserError(failure, isOnline())
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

    private fun resourceFor(kind: UserErrorKind, fallbackRes: Int, missingRes: Int? = null): Int =
        when (kind) {
            UserErrorKind.SecureConnectionFailed -> R.string.error_secure_connection_failed
            UserErrorKind.NetworkUnavailable -> R.string.error_network_unavailable
            UserErrorKind.InstanceUnreachable -> R.string.error_instance_unreachable
            UserErrorKind.ServerSlow -> R.string.error_server_slow
            UserErrorKind.ProviderBlocked -> R.string.error_provider_blocked
            UserErrorKind.ContentBlocked -> R.string.error_content_blocked
            UserErrorKind.SignInAgain -> R.string.error_sign_in_again
            UserErrorKind.PermissionDenied -> R.string.error_permission_denied
            UserErrorKind.ContentUnavailable -> missingRes ?: R.string.error_content_unavailable
            UserErrorKind.LoginCodeExpired -> R.string.error_login_code_expired
            UserErrorKind.LoginCodeDenied -> R.string.error_login_code_denied
            UserErrorKind.LoginCodeNotFound -> R.string.error_login_code_not_found
            UserErrorKind.Conflict -> R.string.error_conflict
            UserErrorKind.RateLimited -> R.string.error_rate_limited
            UserErrorKind.ServerUnavailable -> R.string.error_server_unavailable
            UserErrorKind.InstanceUnavailable -> R.string.error_instance_unavailable
            UserErrorKind.InstanceIncompatible -> R.string.error_instance_incompatible
            UserErrorKind.Fallback -> fallbackRes
        }

    private fun isOnline(): Boolean {
        val connectivity = context.getSystemService(ConnectivityManager::class.java) ?: return true
        val capabilities = connectivity.getNetworkCapabilities(connectivity.activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}

data class UserErrorDetails(
    val message: String,
    val requestId: String?,
)

private val SAFE_REQUEST_ID = Regex("[A-Za-z0-9._:-]{1,128}")
