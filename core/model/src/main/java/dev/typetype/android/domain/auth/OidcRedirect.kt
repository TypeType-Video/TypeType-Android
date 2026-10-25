package dev.typetype.android.domain.auth

import java.net.URI

// The app's own scheme receives the sign-in callback, so it follows the installed package (".debug" included).
class OidcRedirect(val scheme: String) {
    val uri: String = "$scheme://oidc/callback"

    fun matches(value: String): Boolean = runCatching {
        val callback = URI(value)
        callback.scheme == scheme && callback.host == "oidc" && callback.path == "/callback"
    }.getOrDefault(false)
}
