package video.typetype.tv.data

import android.net.Uri
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import video.typetype.sdk.core.TypeTypeResult

public fun TvViewModel.beginOidc(openAuthorizationUrl: (String) -> Unit) {
    if (mutableState.value.metadata?.oidcEnabled != true) return
    viewModelScope.launch {
        mutableState.value = mutableState.value.copy(isLoading = true, errorMessage = null)
        when (val result = client.auth.oidcStart()) {
            is TypeTypeResult.Success -> {
                val authorization = parseOidcAuthorization(result.value.authorizationUrl)
                if (authorization == null) {
                    mutableState.value = mutableState.value.copy(
                        isLoading = false,
                        errorMessage = "The instance returned an invalid sign-in link",
                    )
                    return@launch
                }
                pendingOidcAuthorization = authorization
                try {
                    openAuthorizationUrl(authorization.url)
                    mutableState.value = mutableState.value.copy(isLoading = false)
                } catch (_: Exception) {
                    pendingOidcAuthorization = null
                    mutableState.value = mutableState.value.copy(
                        isLoading = false,
                        errorMessage = "Unable to open the sign-in link",
                    )
                }
            }
            is TypeTypeResult.Failure -> mutableState.value = mutableState.value.copy(
                isLoading = false,
                errorMessage = result.error.toUserMessage(),
            )
        }
    }
}

public fun TvViewModel.handleOidcCallback(uri: Uri): Boolean {
    if (uri.scheme != "video.typetype.tv" || uri.host != "auth" || uri.path != "/callback") return false
    val authorization = pendingOidcAuthorization
    if (authorization == null) {
        mutableState.value = mutableState.value.copy(errorMessage = "This sign-in attempt has expired")
        return true
    }
    val callback = parseOidcCallback(uri.toString(), authorization.state, authorization.redirectUri)
    pendingOidcAuthorization = null
    if (callback == null) {
        mutableState.value = mutableState.value.copy(errorMessage = "The sign-in response could not be verified")
        return true
    }
    viewModelScope.launch {
        mutableState.value = mutableState.value.copy(isLoading = true, errorMessage = null)
        when (val result = client.auth.oidcCallback(callback)) {
            is TypeTypeResult.Success -> {
                mutableState.value = mutableState.value.copy(
                    authStatus = TvAuthStatus.AUTHENTICATED,
                    isLoading = true,
                )
                loadAuthenticatedContent()
            }
            is TypeTypeResult.Failure -> mutableState.value = mutableState.value.copy(
                isLoading = false,
                errorMessage = result.error.toUserMessage(),
            )
        }
    }
    return true
}
