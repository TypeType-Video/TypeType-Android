package dev.typetype.android.data.download

import dev.typetype.android.data.network.ServerError

object DownloadFailureCodes {
    const val Authentication = "authentication"
    const val Cancelled = "cancelled"
    const val InsufficientStorage = "insufficient_storage"
    const val InvalidResponse = "invalid_response"
    const val Network = "network"
    const val Rejected = "rejected"
    const val ServerUnavailable = "server_unavailable"
    const val TimedOut = "timed_out"
    const val Unknown = "unknown"

    fun fromHttp(status: Int, error: ServerError): String = when {
        status == 401 || status == 403 -> Authentication
        status == 507 || error.code == InsufficientStorage -> InsufficientStorage
        status == 429 || status >= 500 -> ServerUnavailable
        else -> Rejected
    }

    fun isRetryable(code: String): Boolean =
        code == Network || code == ServerUnavailable || code == TimedOut
}
