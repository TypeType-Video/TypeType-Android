package dev.typetype.android.data.network

import android.content.Context
import android.security.KeyChain
import android.util.Base64
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import dev.typetype.android.data.account.AccountScopeStore
import dev.typetype.android.domain.server.Server
import dev.typetype.android.domain.server.ServerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 24)
class UserCertificateHttpsAndroidTest {
    @Test
    fun loginAndRefreshUseAUserInstalledCertificateAuthority() {
        val arguments = InstrumentationRegistry.getArguments()
        if (arguments.getString(STAGE_ARGUMENT) == INSTALL_STAGE) {
            launchCertificateInstaller(arguments.getString(CERTIFICATE_ARGUMENT))
            return
        }
        val rawBaseUrl = arguments.getString(BASE_URL_ARGUMENT)
        assumeNotNull(rawBaseUrl)
        val baseUrl = requireNotNull(rawBaseUrl?.toHttpUrlOrNull())
        require(baseUrl.isHttps)
        when (arguments.getString(STAGE_ARGUMENT)) {
            LOGIN_STAGE -> login(baseUrl)
            REFRESH_STAGE -> refresh(baseUrl)
            else -> error("Missing HTTPS test stage")
        }
    }

    private fun launchCertificateInstaller(encodedCertificate: String?) {
        assumeNotNull(encodedCertificate)
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = KeyChain.createInstallIntent().apply {
            putExtra(KeyChain.EXTRA_CERTIFICATE, Base64.decode(encodedCertificate, Base64.DEFAULT))
            putExtra(KeyChain.EXTRA_NAME, "TypeType Android Test CA")
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun login(baseUrl: HttpUrl) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val server = testServer(baseUrl)
        val accountStore = AccountScopeStore(context)
        val jar = cookieJar(context, server, accountStore)
        jar.clear()
        accountStore.clearCurrentAccountId(SERVER_ID)
        jar.beginAuthentication(SERVER_ID, server.baseUrl)

        val response = client(jar).newCall(
            postRequest(
                baseUrl.endpoint("auth/login"),
                """{"identifier":"test","password":"test"}""",
            ),
        ).execute()
        response.use {
            assertEquals(200, it.code)
            assertTrue(it.body.string().contains("login-access-token"))
        }

        jar.completeAuthentication(SERVER_ID, ACCOUNT_ID)
        accountStore.setCurrentAccountId(SERVER_ID, ACCOUNT_ID)
        assertEquals(
            listOf("refresh_token"),
            jar.scoped(SERVER_ID, ACCOUNT_ID, server.baseUrl)
                .loadForRequest(baseUrl.endpoint("auth/refresh"))
                .map { it.name },
        )
    }

    private fun refresh(baseUrl: HttpUrl) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val server = testServer(baseUrl)
        val accountStore = AccountScopeStore(context)
        val recreatedJar = cookieJar(context, server, accountStore)
        try {
            val scopedJar = recreatedJar.scoped(SERVER_ID, ACCOUNT_ID, server.baseUrl)
            val tokenStore = HttpsTestAccessTokenStore("expired-access-token")
            val scope = NetworkRequestScope(SERVER_ID, ACCOUNT_ID, server.baseUrl)
            val authenticatedClient = OkHttpClient.Builder()
                .cookieJar(scopedJar)
                .addInterceptor(ScopedRequestInterceptor(scope) {
                    tokenStore.getAccessToken(SERVER_ID, ACCOUNT_ID)
                })
                .authenticator(
                    ScopedTokenAuthenticator(
                        scope = scope,
                        tokenStore = tokenStore,
                        refreshClient = client(scopedJar),
                        json = Json { ignoreUnknownKeys = true },
                        lock = Any(),
                    ),
                )
                .build()
            val response = authenticatedClient.newCall(
                Request.Builder().url(baseUrl.endpoint("protected")).build(),
            ).execute()
            response.use {
                assertEquals(200, it.code)
                assertTrue(it.body.string().contains("authenticated"))
            }
            assertEquals(
                "refreshed-access-token",
                tokenStore.getAccessToken(SERVER_ID, ACCOUNT_ID),
            )
        } finally {
            recreatedJar.clear()
            accountStore.clearCurrentAccountId(SERVER_ID)
        }
    }

    private fun cookieJar(
        context: Context,
        server: Server,
        accountStore: AccountScopeStore,
    ): PersistentCookieJar = PersistentCookieJar(
        context,
        ApiBaseUrlHolder(HttpsTestServerRepository(server)),
        accountStore,
    )

    private fun testServer(baseUrl: HttpUrl) = Server(
        id = SERVER_ID,
        baseUrl = baseUrl.toString(),
        displayName = "HTTPS certificate test",
        addedAt = 1L,
    )

    private fun client(cookieJar: okhttp3.CookieJar) = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .build()

    private fun postRequest(url: HttpUrl, body: String) = Request.Builder()
        .url(url)
        .post(body.toRequestBody(JSON_MEDIA_TYPE))
        .build()

    private fun HttpUrl.endpoint(path: String): HttpUrl = requireNotNull(resolve(path))

    private companion object {
        const val BASE_URL_ARGUMENT = "typetypeHttpsTestUrl"
        const val STAGE_ARGUMENT = "typetypeHttpsTestStage"
        const val CERTIFICATE_ARGUMENT = "typetypeHttpsTestCertificate"
        const val INSTALL_STAGE = "install"
        const val LOGIN_STAGE = "login"
        const val REFRESH_STAGE = "refresh"
        const val SERVER_ID = "user-ca-https-test-server"
        const val ACCOUNT_ID = "user-ca-https-test-account"
        val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }
}

private class HttpsTestAccessTokenStore(initialToken: String) : ScopedAccessTokenStore {
    private var token: String? = initialToken

    override fun getAccessToken(serverId: String, accountId: String): String? = token

    override fun setAccessToken(serverId: String, accountId: String, token: String?) {
        this.token = token
    }
}

private class HttpsTestServerRepository(server: Server) : ServerRepository {
    private val current = MutableStateFlow<Server?>(server)

    override fun observeServers(): Flow<List<Server>> = MutableStateFlow(listOfNotNull(current.value))

    override fun observeCurrentServer(): Flow<Server?> = current

    override suspend fun getServer(id: String): Server? = current.value?.takeIf { it.id == id }

    override suspend fun addServer(server: Server) {
        current.value = server
    }

    override suspend fun deleteServer(id: String) {
        if (current.value?.id == id) current.value = null
    }

    override suspend fun setCurrentServer(id: String) = Unit

    override suspend fun clearCurrentServer() {
        current.value = null
    }
}
