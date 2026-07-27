package dev.typetype.android.data.network

import dev.typetype.android.core.error.CodedFailure
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import retrofit2.Response

@Serializable
private data class ErrorBody(
    val code: String? = null,
    val error: String? = null,
    val message: String? = null,
)

data class ServerError(
    val message: String,
    val code: String?,
    val statusCode: Int = 0,
    val requestId: String? = null,
)

class ServerResponseException(
    val error: ServerError,
) : IllegalStateException(error.message), CodedFailure {
    override val failureCode: String? = error.code
    override val requestId: String? = error.requestId
    override val statusCode: Int = error.statusCode
}

private val json = Json { ignoreUnknownKeys = true; isLenient = true }

fun extractServerError(response: Response<*>): ServerError {
    val statusCode = response.code()
    val requestId = response.headers()["X-Request-ID"]
        ?.takeIf { it.matches(REQUEST_ID_PATTERN) }
    val errorBodyString = runCatching { response.errorBody()?.string() }.getOrNull()
    if (!errorBodyString.isNullOrBlank()) {
        runCatching { json.decodeFromString(ErrorBody.serializer(), errorBodyString) }
            .getOrNull()
            ?.let { parsed ->
                val msg = parsed.error?.takeIf { it.isNotBlank() }
                    ?: parsed.message?.takeIf { it.isNotBlank() }
                if (msg != null) {
                    return ServerError(
                        message = msg,
                        code = parsed.code?.takeIf { it.isNotBlank() }
                            ?: msg.takeIf(STABLE_ERROR_CODE_PATTERN::matches),
                        statusCode = statusCode,
                        requestId = requestId,
                    )
                }
            }
    }
    return ServerError(GENERIC_SERVER_FAILURE, null, statusCode, requestId)
}

fun serverResponseException(response: Response<*>): ServerResponseException =
    ServerResponseException(extractServerError(response))

fun Response<*>.requireSuccessfulResponse() {
    if (!isSuccessful) throw serverResponseException(this)
}

private val REQUEST_ID_PATTERN = Regex("[A-Za-z0-9._:-]{1,128}")
private val STABLE_ERROR_CODE_PATTERN = Regex("[A-Z][A-Z0-9_]{2,63}")
private const val GENERIC_SERVER_FAILURE = "Server request failed"
