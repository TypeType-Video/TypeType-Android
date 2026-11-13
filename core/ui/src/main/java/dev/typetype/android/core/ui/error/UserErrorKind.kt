package dev.typetype.android.core.ui.error

import dev.typetype.android.core.error.CodedFailure
import java.io.IOException
import java.io.InterruptedIOException
import javax.net.ssl.SSLException

internal enum class UserErrorKind {
    SecureConnectionFailed,
    NetworkUnavailable,
    InstanceUnreachable,
    ServerSlow,
    ProviderBlocked,
    ContentBlocked,
    SignInAgain,
    PermissionDenied,
    ContentUnavailable,
    Conflict,
    RateLimited,
    ServerUnavailable,
    InstanceUnavailable,
    InstanceIncompatible,
    Fallback,
}

// "No connection" is only shown when the phone really is offline; otherwise a transport failure means the instance
// could not be reached or answered too slowly.
internal fun classifyUserError(failure: Throwable?, online: Boolean = true): UserErrorKind {
    if (failure.hasCause<SSLException>()) return UserErrorKind.SecureConnectionFailed
    if (failure is IOException) {
        return when {
            !online -> UserErrorKind.NetworkUnavailable
            failure.hasCause<InterruptedIOException>() -> UserErrorKind.ServerSlow
            else -> UserErrorKind.InstanceUnreachable
        }
    }
    return when (failure?.message) {
        "This account needs to sign in again",
        "No account is currently selected",
        -> UserErrorKind.SignInAgain
        "Instance not found", "Server not found" -> UserErrorKind.InstanceUnavailable
        else -> classifyCodedFailure(failure)
    }
}

private inline fun <reified T : Throwable> Throwable?.hasCause(): Boolean =
    generateSequence(this) { it.cause }.any { it is T }

private fun classifyCodedFailure(failure: Throwable?): UserErrorKind {
    if (failure?.message?.contains("compatible TypeType instance") == true ||
        failure?.message?.startsWith("This instance uses API ") == true
    ) {
        return UserErrorKind.InstanceIncompatible
    }
    val coded = failure as? CodedFailure ?: return UserErrorKind.Fallback
    return classifyUserError(coded.failureCode, coded.statusCode)
}

internal fun classifyUserError(failureCode: String?, statusCode: Int?): UserErrorKind =
    when (failureCode?.lowercase()) {
        "client_network_unavailable" -> UserErrorKind.NetworkUnavailable
        "authentication_required", "invalid_token", "unauthorized" -> UserErrorKind.SignInAgain
        "rate_limited" -> UserErrorKind.RateLimited
        "provider_access_blocked", "token_service_unavailable" -> UserErrorKind.ProviderBlocked
        "upstream_timeout", "sabr_upstream_timeout" -> UserErrorKind.ServerSlow
        "content_blocked" -> UserErrorKind.ContentBlocked
        "content_unavailable", "private_content" -> UserErrorKind.ContentUnavailable
        else -> classifyStatus(statusCode)
    }

private fun classifyStatus(status: Int?): UserErrorKind = when (status) {
    401 -> UserErrorKind.SignInAgain
    403 -> UserErrorKind.PermissionDenied
    404, 410 -> UserErrorKind.ContentUnavailable
    408 -> UserErrorKind.NetworkUnavailable
    409 -> UserErrorKind.Conflict
    429 -> UserErrorKind.RateLimited
    504 -> UserErrorKind.ServerSlow
    in 500..599 -> UserErrorKind.ServerUnavailable
    else -> UserErrorKind.Fallback
}
