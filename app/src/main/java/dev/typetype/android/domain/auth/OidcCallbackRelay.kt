package dev.typetype.android.domain.auth

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

@Singleton
class OidcCallbackRelay @Inject constructor() {
    private val callbackUrls = Channel<String>(Channel.CONFLATED)
    @Volatile
    private var pendingCallbackUrl: String? = null

    val callbacks: Flow<String> = callbackUrls.receiveAsFlow()
    val hasPendingCallback: Boolean
        get() = pendingCallbackUrl != null

    fun submit(callbackUrl: String) {
        pendingCallbackUrl = callbackUrl
        callbackUrls.trySend(callbackUrl)
    }

    fun markConsumed(callbackUrl: String) {
        if (pendingCallbackUrl == callbackUrl) {
            pendingCallbackUrl = null
        }
    }
}
