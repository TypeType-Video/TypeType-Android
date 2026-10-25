package dev.typetype.android.domain.auth

import java.net.URI

class OidcRedirect(val scheme: String) {
    val uri: String = "$scheme://oidc/callback"

    fun matches(value: String): Boolean = runCatching {
        val callback = URI(value)
        callback.scheme == scheme && callback.host == "oidc" && callback.path == "/callback"
    }.getOrDefault(false)
}
