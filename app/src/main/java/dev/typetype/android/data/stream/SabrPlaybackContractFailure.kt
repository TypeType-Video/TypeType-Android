package dev.typetype.android.data.stream

import dev.typetype.android.core.error.CodedFailure
import retrofit2.Response

internal fun Response<*>.rejectSabrRedirect(message: String) {
    if (code() in 300..399) sabrContractMismatch(message)
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
