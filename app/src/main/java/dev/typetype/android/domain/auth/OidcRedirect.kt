package dev.typetype.android.domain.auth

import dev.typetype.android.BuildConfig
import java.net.URI

object OidcRedirect {
    val scheme: String = BuildConfig.APPLICATION_ID
    val uri: String = "$scheme://oidc/callback"

    fun matches(value: String): Boolean = runCatching {
        val callback = URI(value)
        callback.scheme == scheme && callback.host == "oidc" && callback.path == "/callback"
    }.getOrDefault(false)
}
