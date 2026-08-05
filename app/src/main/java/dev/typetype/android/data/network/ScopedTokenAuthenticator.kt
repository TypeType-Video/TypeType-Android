package dev.typetype.android.data.network

import dev.typetype.android.data.network.dto.RefreshRequest
import dev.typetype.android.data.network.dto.SessionResponse
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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
            if (response.code == 401) return null
            val requestId = response.header("X-Request-ID")
            val payload = response.body.string()
            if (!response.isSuccessful) {
                throw SessionRefreshUnavailableException(
                    statusCode = response.code,
                    failureCode = payload.stableErrorCode(),
                    requestId = requestId,
                )
            }
            try {
                json.decodeFromString(SessionResponse.serializer(), payload)
                    .accessToken
                    .takeIf(String::isNotBlank)
                    ?: error("The refresh response contained an empty access token")
            } catch (error: Exception) {
                throw SessionRefreshUnavailableException(
                    statusCode = response.code,
                    failureCode = "auth_refresh_payload_invalid",
                    requestId = requestId,
                    cause = error,
                )
            }
        }
    }

    private fun String.stableErrorCode(): String? = runCatching {
        json.parseToJsonElement(this).jsonObject["code"]?.jsonPrimitive?.contentOrNull
    }.getOrNull()?.takeIf(STABLE_REFRESH_CODE::matches)
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

private val STABLE_REFRESH_CODE = Regex("[A-Za-z][A-Za-z0-9._:-]{1,127}")
