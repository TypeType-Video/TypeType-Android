package video.typetype.tv.data

import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import video.typetype.sdk.core.OidcCallback

internal const val TV_OIDC_REDIRECT_URI = "video.typetype.tv://auth/callback"

internal data class TvOidcAuthorization(
    val url: String,
    val state: String,
    val redirectUri: String,
)

internal fun parseOidcAuthorization(rawUrl: String): TvOidcAuthorization? {
    val value = rawUrl.trim()
    val uri = runCatching { URI(value) }.getOrNull() ?: return null
    if (uri.scheme != "https" || uri.host.isNullOrBlank()) return null
    val query = uri.queryParameters()
    val state = query["state"]?.takeIf(String::isNotBlank) ?: return null
    val redirectUri = query["redirect_uri"]
        ?.takeIf(String::isNotBlank)
        ?: TV_OIDC_REDIRECT_URI
    if (!redirectUri.equals(TV_OIDC_REDIRECT_URI, ignoreCase = true)) return null
    return TvOidcAuthorization(value, state, redirectUri)
}

internal fun parseOidcCallback(rawUri: String, expectedState: String, redirectUri: String): OidcCallback? {
    val uri = runCatching { URI(rawUri) }.getOrNull() ?: return null
    if (uri.scheme != "video.typetype.tv" || uri.host != "auth" || uri.path != "/callback") return null
    val query = uri.queryParameters()
    val code = query["code"]?.takeIf(String::isNotBlank) ?: return null
    val state = query["state"]?.takeIf(String::isNotBlank) ?: return null
    if (state != expectedState) return null
    return OidcCallback(code = code, state = state, redirectUri = redirectUri)
}

private fun URI.queryParameters(): Map<String, String> = rawQuery.orEmpty()
    .split('&')
    .mapNotNull { parameter ->
        val parts = parameter.split('=', limit = 2)
        if (parts.size != 2) return@mapNotNull null
        val name = decode(parts[0]).takeIf(String::isNotBlank) ?: return@mapNotNull null
        name to decode(parts[1])
    }
    .toMap()

private fun decode(value: String): String = runCatching {
    URLDecoder.decode(value, StandardCharsets.UTF_8.name())
}.getOrDefault("")
