package dev.typetype.android.data.network

import dev.typetype.android.data.network.dto.RefreshRequest
import dev.typetype.android.data.network.dto.SessionResponse
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route

@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenStore: AccessTokenStore,
    private val baseUrlHolder: ApiBaseUrlHolder,
    @param:Named("refresh") private val refreshClient: Provider<OkHttpClient>,
    private val json: Json,
) : Authenticator {

    private val mutex = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.responseCount > 1) return null
        if (response.request.header("Authorization") == null) return null
        if (response.request.url.encodedPath.endsWith("/auth/refresh")) return null

        synchronized(mutex) {
            val currentToken = response.request.header("Authorization")?.removePrefix("Bearer ")
            val storedToken = tokenStore.getAccessToken()
            if (storedToken != null && storedToken != currentToken) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $storedToken")
                    .build()
            }
            val newToken = runCatching { refreshAccessToken() }.getOrNull() ?: return null
            tokenStore.setAccessToken(newToken)
            return response.request.newBuilder()
                .header("Authorization", "Bearer $newToken")
                .build()
        }
    }

    private fun refreshAccessToken(): String? {
        val baseUrl = baseUrlHolder.currentBaseUrl?.trimEnd('/') ?: return null
        val body = json.encodeToString(RefreshRequest.serializer(), RefreshRequest())
            .toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$baseUrl/auth/refresh")
            .post(body)
            .build()
        refreshClient.get().newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val text = resp.body.string()
            return runCatching {
                json.decodeFromString(SessionResponse.serializer(), text).accessToken
            }.getOrNull()
        }
    }
}

private val Response.responseCount: Int
    get() {
        var count = 1
        var prior = priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
