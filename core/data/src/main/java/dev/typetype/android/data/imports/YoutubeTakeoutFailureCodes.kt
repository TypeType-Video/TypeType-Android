package dev.typetype.android.data.imports

import java.io.FileNotFoundException
import java.io.IOException

internal object YoutubeTakeoutFailureCodes {
    const val Authentication = "YOUTUBE_IMPORT_AUTHENTICATION"
    const val AccountRequired = "YOUTUBE_IMPORT_ACCOUNT_REQUIRED"
    const val Cancelled = "YOUTUBE_IMPORT_CANCELLED"
    const val InvalidArchive = "YOUTUBE_IMPORT_INVALID_ARCHIVE"
    const val InvalidResponse = "YOUTUBE_IMPORT_INVALID_RESPONSE"
    const val JobFailed = "YOUTUBE_IMPORT_JOB_FAILED"
    const val JobNotFound = "YOUTUBE_IMPORT_JOB_NOT_FOUND"
    const val Network = "YOUTUBE_IMPORT_NETWORK"
    const val Permission = "YOUTUBE_IMPORT_PERMISSION"
    const val ScopeChanged = "YOUTUBE_IMPORT_SCOPE_CHANGED"
    const val ServerUnavailable = "YOUTUBE_IMPORT_SERVER_UNAVAILABLE"
    const val TimedOut = "YOUTUBE_IMPORT_TIMED_OUT"
    const val TooLarge = "YOUTUBE_IMPORT_TOO_LARGE"
    const val Unsupported = "YOUTUBE_IMPORT_UNSUPPORTED"
    const val Unknown = "YOUTUBE_IMPORT_UNKNOWN"

    fun fromHttp(statusCode: Int, serverCode: String?, uploadRequest: Boolean = false): String =
        serverCode ?: when {
            uploadRequest && statusCode == 404 -> Unsupported
            statusCode in setOf(400, 413, 415, 422) -> InvalidArchive
            statusCode == 401 || statusCode == 403 -> Authentication
            statusCode == 404 -> JobNotFound
            statusCode in 500..599 -> ServerUnavailable
            else -> Unknown
        }

    fun isRetryable(statusCode: Int): Boolean =
        statusCode == 408 || statusCode == 429 || statusCode in 500..599

    fun fromThrowable(failure: Throwable): String {
        var current: Throwable? = failure
        var containsIoFailure = false
        while (current != null) {
            when {
                current is SecurityException -> return Permission
                current is FileNotFoundException && current.message == "IMPORT_FILE_UNAVAILABLE" -> return Permission
                current.message == "IMPORT_FILE_TOO_LARGE" -> return TooLarge
                current is IOException -> containsIoFailure = true
            }
            current = current.cause
        }
        return if (containsIoFailure) Network else Unknown
    }
}
