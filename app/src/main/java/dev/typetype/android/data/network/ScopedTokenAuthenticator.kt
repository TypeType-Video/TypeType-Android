package dev.typetype.android.data.network

import dev.typetype.android.data.network.dto.RefreshRequest
import dev.typetype.android.data.network.dto.SessionResponse
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route

class ScopedTokenAuthenticator(
    private val scope: NetworkRequestScope,
    private val tokenStore: ScopedAccessTokenStore,
    private val refreshClient: okhttp3.OkHttpClient,
    private val json: Json,
    private val lock: Any,
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.priorResponseCount > 1) return null
        if (response.request.header("Authorization") == null) return null
        if (response.request.url.encodedPath.endsWith("/auth/refresh")) return null
        synchronized(lock) {
            val requestToken = response.request.header("Authorization")?.removePrefix("Bearer ")
            val storedToken = tokenStore.getAccessToken(scope.serverId, scope.accountId)
            if (storedToken != null && storedToken != requestToken) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $storedToken")
                    .build()
            }
            val refreshed = refresh() ?: return null
            tokenStore.setAccessToken(scope.serverId, scope.accountId, refreshed)
            return response.request.newBuilder()
                .header("Authorization", "Bearer $refreshed")
                .build()
        }
    }

    private fun refresh(): String? {
        val body = json.encodeToString(RefreshRequest.serializer(), RefreshRequest())
            .toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("${scope.baseUrl.trimEnd('/')}/auth/refresh")
            .post(body)
            .build()
        return refreshClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            runCatching {
                json.decodeFromString(SessionResponse.serializer(), response.body.string()).accessToken
            }.getOrNull()
        }
    }
}

private val Response.priorResponseCount: Int
    get() {
        var count = 1
        var prior = priorResponse
        while (prior != null) {
            count += 1
            prior = prior.priorResponse
        }
        return count
    }
