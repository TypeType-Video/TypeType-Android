package dev.typetype.android.data.network

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.typetype.android.data.account.AccountScopeStore
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import org.json.JSONArray
import org.json.JSONObject

@Singleton
class PersistentCookieJar @Inject constructor(
    @ApplicationContext context: Context,
    private val baseUrlHolder: ApiBaseUrlHolder,
    private val accountScopeStore: AccountScopeStore,
) : CookieJar {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val cookies: MutableMap<CookieScope, MutableList<Cookie>> = mutableMapOf()
    private val legacyCookies: MutableMap<String, MutableList<Cookie>> = mutableMapOf()
    private var pendingEndpoint: CurrentServerEndpoint? = null

    init {
        loadCookies()
    }

    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val scope = scopeFor(url) ?: return
        save(scope, cookies)
    }

    private fun save(scope: CookieScope, cookies: List<Cookie>) {
        val bucket = this.cookies.getOrPut(scope) { mutableListOf() }
        for (incoming in cookies) {
            bucket.removeAll { it.name == incoming.name && it.path == incoming.path }
            if (!incoming.hasExpired()) {
                bucket.add(incoming)
            }
        }
        persist()
    }

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val scope = scopeFor(url) ?: return emptyList()
        return load(scope, url)
    }

    private fun load(scope: CookieScope, url: HttpUrl): List<Cookie> {
        val bucket = cookies.getOrPut(scope) {
            legacyCookies.remove(url.host) ?: mutableListOf()
        }
        val now = System.currentTimeMillis()
        val expired = bucket.filter { it.expiresAt <= now }
        if (expired.isNotEmpty()) {
            bucket.removeAll(expired.toSet())
            persist()
        }
        return bucket.filter { it.matches(url) }
    }

    fun scoped(serverId: String, accountId: String, baseUrl: String): CookieJar {
        val endpoint = CurrentServerEndpoint(serverId, baseUrl)
        return object : CookieJar {
            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                if (!endpoint.owns(url)) return
                saveExplicit(CookieScope(serverId, accountId, url.host), cookies)
            }

            override fun loadForRequest(url: HttpUrl): List<Cookie> {
                if (!endpoint.owns(url)) return emptyList()
                return loadExplicit(CookieScope(serverId, accountId, url.host), url)
            }
        }
    }

    @Synchronized
    private fun saveExplicit(scope: CookieScope, cookies: List<Cookie>) = save(scope, cookies)

    @Synchronized
    private fun loadExplicit(scope: CookieScope, url: HttpUrl): List<Cookie> = load(scope, url)

    @Synchronized
    fun clear() {
        cookies.clear()
        legacyCookies.clear()
        prefs.edit { clear() }
    }

    @Synchronized
    fun beginAuthentication(serverId: String, baseUrl: String) {
        pendingEndpoint = CurrentServerEndpoint(serverId, baseUrl)
        cookies.keys.removeAll { it.serverId == serverId && it.accountId == PENDING_ACCOUNT_ID }
        persist()
    }

    @Synchronized
    fun resumeAuthentication(serverId: String, baseUrl: String) {
        pendingEndpoint = CurrentServerEndpoint(serverId, baseUrl)
    }

    @Synchronized
    fun completeAuthentication(serverId: String, accountId: String) {
        val pending = cookies.filterKeys {
            it.serverId == serverId && it.accountId == PENDING_ACCOUNT_ID
        }
        for ((scope, values) in pending) {
            cookies[scope.copy(accountId = accountId)] = values
            cookies.remove(scope)
        }
        pendingEndpoint = null
        persist()
    }

    @Synchronized
    fun cancelAuthentication(serverId: String) {
        cookies.keys.removeAll { it.serverId == serverId && it.accountId == PENDING_ACCOUNT_ID }
        if (pendingEndpoint?.serverId == serverId) pendingEndpoint = null
        persist()
    }

    @Synchronized
    fun clearCurrentSession(serverId: String, host: String) {
        val accountId = accountScopeStore.getCurrentAccountId(serverId) ?: PENDING_ACCOUNT_ID
        cookies.keys.removeAll {
            it.serverId == serverId && it.accountId == accountId && it.host == host.lowercase()
        }
        persist()
    }

    @Synchronized
    fun clearAccount(serverId: String, accountId: String) {
        cookies.keys.removeAll { it.serverId == serverId && it.accountId == accountId }
        persist()
    }

    private fun Cookie.hasExpired(): Boolean = expiresAt <= System.currentTimeMillis()

    private fun scopeFor(url: HttpUrl): CookieScope? {
        val endpoint = pendingEndpoint?.takeIf { it.owns(url) }
            ?: baseUrlHolder.currentEndpoint
            ?: return null
        if (!endpoint.owns(url)) return null
        val accountId = if (pendingEndpoint?.serverId == endpoint.serverId) {
            PENDING_ACCOUNT_ID
        } else {
            accountScopeStore.getCurrentAccountId(endpoint.serverId) ?: PENDING_ACCOUNT_ID
        }
        return CookieScope(endpoint.serverId, accountId, url.host)
    }

    private fun loadCookies() {
        val raw = prefs.getString(KEY, null) ?: return
        runCatching {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val cookie = obj.toCookie() ?: continue
                val serverId = obj.optString("serverId")
                val accountId = obj.optString("accountId")
                val host = obj.optString("responseHost", cookie.domain).lowercase()
                if (serverId.isBlank() || accountId.isBlank()) {
                    legacyCookies.getOrPut(host) { mutableListOf() }.add(cookie)
                } else {
                    cookies.getOrPut(CookieScope(serverId, accountId, host)) { mutableListOf() }
                        .add(cookie)
                }
            }
        }
    }

    private fun persist() {
        val arr = JSONArray()
        for ((scope, list) in cookies) {
            for (cookie in list) {
                arr.put(cookie.toJson(scope))
            }
        }
        for ((host, list) in legacyCookies) {
            for (cookie in list) {
                arr.put(cookie.toJson(CookieScope("", "", host)))
            }
        }
        prefs.edit { putString(KEY, arr.toString()) }
    }

    private fun Cookie.toJson(scope: CookieScope): JSONObject = JSONObject().apply {
        put("serverId", scope.serverId)
        put("accountId", scope.accountId)
        put("responseHost", scope.host)
        put("name", name)
        put("value", value)
        put("domain", domain)
        put("path", path)
        put("expiresAt", expiresAt)
        put("secure", secure)
        put("httpOnly", httpOnly)
        put("hostOnly", hostOnly)
    }

    private fun JSONObject.toCookie(): Cookie? = runCatching {
        val builder = Cookie.Builder()
            .name(getString("name"))
            .value(getString("value"))
            .path(optString("path", "/"))
            .expiresAt(optLong("expiresAt", Long.MAX_VALUE))
        val domain = getString("domain")
        if (optBoolean("hostOnly", false)) builder.hostOnlyDomain(domain) else builder.domain(domain)
        if (optBoolean("secure", false)) builder.secure()
        if (optBoolean("httpOnly", false)) builder.httpOnly()
        builder.build()
    }.getOrNull()

    private data class CookieScope(
        val serverId: String,
        val accountId: String,
        val host: String,
    )

    private companion object {
        const val PREFS = "typetype_cookies"
        const val KEY = "cookies_json"
        const val PENDING_ACCOUNT_ID = "__pending__"
    }
}
