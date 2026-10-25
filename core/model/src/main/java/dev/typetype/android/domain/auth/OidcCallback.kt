package dev.typetype.android.domain.auth

import java.net.URI
import java.net.URLDecoder

data class OidcCallback(
    val code: String,
    val state: String,
)

object OidcCallbackParser {
    fun parse(callbackUrl: String, expectedScheme: String): OidcCallback {
        val callback = URI(callbackUrl)
        require(callback.scheme == expectedScheme) { "Unexpected OIDC callback scheme" }
        require(callback.host == "oidc" && callback.path == "/callback") {
            "Unexpected OIDC callback destination"
        }
        val decoded = callback.rawQuery.orEmpty()
            .split('&')
            .mapNotNull(::decodeQueryParameter)
        require(decoded.groupingBy(Pair<String, String>::first).eachCount().values.none { it > 1 }) {
            "OIDC callback contains duplicate parameters"
        }
        val query = decoded.toMap()
        query["error"]?.let { error("OIDC authorization was rejected: $it") }
        return OidcCallback(
            code = query["code"] ?: error("OIDC callback is missing the code"),
            state = query["state"] ?: error("OIDC callback is missing the state"),
        )
    }

    private fun decodeQueryParameter(part: String): Pair<String, String>? {
        val pieces = part.split('=', limit = 2)
        if (pieces.size != 2) return null
        return URLDecoder.decode(pieces[0], "UTF-8") to URLDecoder.decode(pieces[1], "UTF-8")
    }
}
