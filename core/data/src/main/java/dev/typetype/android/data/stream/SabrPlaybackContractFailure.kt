package dev.typetype.android.data.stream

import dev.typetype.android.core.error.CodedFailure
import dev.typetype.android.data.network.ServerResponseException
import dev.typetype.android.data.network.extractServerError
import retrofit2.Response

const val SABR_PLAYBACK_NOT_FOUND = "sabr_playback_not_found"

internal fun Response<*>.rejectSabrRedirect(message: String) {
    if (code() in 300..399) sabrContractMismatch(message)
}

// The server answers an evicted idle session with a 404 and its generic "error" code.
internal fun sabrSessionResponseException(response: Response<*>): ServerResponseException {
    val error = extractServerError(response)
    return ServerResponseException(
        if (response.code() == HTTP_NOT_FOUND) error.copy(code = SABR_PLAYBACK_NOT_FOUND) else error,
    )
}

internal fun sabrContractMismatch(message: String, cause: Throwable? = null): Nothing =
    throw SabrPlaybackContractFailure(message, "youtube_sabr_contract_mismatch", cause)

internal fun sabrPlaybackFailure(message: String, code: String): IllegalStateException =
    SabrPlaybackContractFailure(message, code)

private class SabrPlaybackContractFailure(
    message: String,
    override val failureCode: String,
    cause: Throwable? = null,
) : IllegalStateException(message, cause), CodedFailure {
    override val requestId: String? = null
    override val statusCode: Int? = null
}

private const val HTTP_NOT_FOUND = 404
