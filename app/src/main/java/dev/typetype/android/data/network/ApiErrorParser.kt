package dev.typetype.android.data.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import retrofit2.Response

@Serializable
private data class ErrorBody(
    val error: String? = null,
    val message: String? = null,
)

private val json = Json { ignoreUnknownKeys = true; isLenient = true }

fun extractServerErrorMessage(response: Response<*>): String {
    val errorBodyString = runCatching { response.errorBody()?.string() }.getOrNull()
    if (!errorBodyString.isNullOrBlank()) {
        runCatching { json.decodeFromString(ErrorBody.serializer(), errorBodyString) }
            .getOrNull()
            ?.let { parsed ->
                val msg = parsed.error?.takeIf { it.isNotBlank() }
                    ?: parsed.message?.takeIf { it.isNotBlank() }
                if (msg != null) return msg
            }
        if (errorBodyString.length in 1..280) return errorBodyString.trim()
    }
    val statusText = response.message().takeIf { it.isNotBlank() }
    return statusText ?: "HTTP ${response.code()}"
}
