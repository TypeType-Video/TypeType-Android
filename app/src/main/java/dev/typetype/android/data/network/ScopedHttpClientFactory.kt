package dev.typetype.android.data.network

import dev.typetype.android.data.diagnostics.DiagnosticsInterceptor
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient

@Singleton
class ScopedHttpClientFactory @Inject constructor(
    @param:Named("refresh") private val baseClient: OkHttpClient,
    private val persistentCookieJar: PersistentCookieJar,
    private val tokenStore: AccessTokenStore,
    private val diagnosticsInterceptor: DiagnosticsInterceptor,
    private val json: Json,
) {
    private val refreshLocks = ConcurrentHashMap<String, Any>()

    fun create(
        baseUrl: String,
        serverId: String,
        accountId: String,
        token: String?,
    ): OkHttpClient {
        val scope = NetworkRequestScope(serverId, accountId, baseUrl)
        val cookieJar = persistentCookieJar.scoped(serverId, accountId, baseUrl)
        val refreshClient = scopedBuilder(scope, authenticated = false)
            .cookieJar(cookieJar)
            .build()
        val builder = scopedBuilder(scope, authenticated = token != null)
            .cookieJar(cookieJar)
        if (token != null) {
            builder.authenticator(
                ScopedTokenAuthenticator(
                    scope = scope,
                    tokenStore = tokenStore,
                    refreshClient = refreshClient,
                    json = json,
                    lock = refreshLocks.getOrPut("$serverId\u0000$accountId") { Any() },
                ),
            )
        }
        return builder.build()
    }

    private fun scopedBuilder(scope: NetworkRequestScope, authenticated: Boolean): OkHttpClient.Builder {
        val builder = baseClient.newBuilder()
        builder.interceptors().removeAll { it is DiagnosticsInterceptor }
        builder.addInterceptor(
            ScopedRequestInterceptor(scope) {
                if (authenticated) tokenStore.getAccessToken(scope.serverId, scope.accountId) else null
            },
        )
        builder.addInterceptor(diagnosticsInterceptor)
        return builder
    }
}
