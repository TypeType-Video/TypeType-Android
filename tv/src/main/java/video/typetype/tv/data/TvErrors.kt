package video.typetype.tv.data

import video.typetype.sdk.core.TypeTypeError

internal fun TypeTypeError.toUserMessage(): String = when (this) {
    is TypeTypeError.Http -> httpSummary(status, message).withDiagnostic(status, code, requestId)
    is TypeTypeError.Network -> "TypeType could not be reached. Check your connection and try again."
        .withRequestId(requestId)
    is TypeTypeError.Serialization -> "TypeType returned a response this app could not read."
        .withRequestId(requestId)
    is TypeTypeError.InvalidRequest -> message.withRequestId(requestId)
    is TypeTypeError.Authentication -> (message ?: "Sign in again to continue.")
        .withDiagnostic(status, code, requestId)
}

private fun httpSummary(status: Int, serverMessage: String?): String = when (status) {
    400 -> serverMessage?.takeIf(String::isNotBlank) ?: "This request could not be completed."
    401, 403 -> "Sign in again to continue."
    404 -> "This content is no longer available."
    409 -> serverMessage?.takeIf(String::isNotBlank) ?: "This action conflicts with a newer change."
    429 -> "TypeType is receiving too many requests. Try again in a moment."
    in 500..599 -> "TypeType is temporarily unavailable. Try again in a moment."
    else -> serverMessage?.takeIf(String::isNotBlank) ?: "TypeType could not complete this request."
}

private fun String.withDiagnostic(status: Int, code: String?, requestId: String?): String = buildString {
    append(this@withDiagnostic)
    append("\nError ").append(status)
    code?.let { append(" · ").append(it) }
    requestId?.let { append(" · request ").append(it) }
}

private fun String.withRequestId(requestId: String?): String = buildString {
    append(this@withRequestId)
    requestId?.let { append("\nRequest ").append(it) }
}
