package dev.typetype.android.data.network

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.typetype.android.data.account.AccountScopeStore
import dev.typetype.android.domain.server.Server
import dev.typetype.android.domain.server.ServerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PersistentCookieJarTest {
    @Test
    fun oidcCookieSurvivesCallbackResumeAndMovesToAuthenticatedScope() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val server = Server(
            id = "cookie-test-instance",
            baseUrl = "https://instance.test/api/",
            displayName = "Cookie test",
            addedAt = 1L,
        )
        val accountStore = AccountScopeStore(context)
        val jar = PersistentCookieJar(
            context,
            ApiBaseUrlHolder(FakeServerRepository(server)),
            accountStore,
        )
        val requestUrl = "${server.baseUrl}auth/oidc/start".toHttpUrl()
        val cookie = Cookie.Builder()
            .name("refresh_token")
            .value("opaque-test-value")
            .hostOnlyDomain(requestUrl.host)
            .path("/")
            .expiresAt(System.currentTimeMillis() + 60_000L)
            .secure()
            .httpOnly()
            .build()

        try {
            jar.clear()
            jar.beginAuthentication(server.id, server.baseUrl)
            jar.saveFromResponse(requestUrl, listOf(cookie))
            jar.resumeAuthentication(server.id, server.baseUrl)
            jar.completeAuthentication(server.id, "account-a")

            val recreatedJar = PersistentCookieJar(
                context,
                ApiBaseUrlHolder(FakeServerRepository(server)),
                accountStore,
            )
            val stored = recreatedJar.scoped(server.id, "account-a", server.baseUrl)
                .loadForRequest(requestUrl)

            assertEquals(listOf("refresh_token"), stored.map { it.name })
            assertTrue(
                recreatedJar.scoped(server.id, "account-b", server.baseUrl)
                    .loadForRequest(requestUrl)
                    .isEmpty(),
            )
        } finally {
            jar.clear()
        }
    }
}

private class FakeServerRepository(server: Server) : ServerRepository {
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
