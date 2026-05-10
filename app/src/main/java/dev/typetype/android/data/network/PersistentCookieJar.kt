package dev.typetype.android.data.network

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
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
) : CookieJar {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val cookies: MutableMap<String, MutableList<Cookie>> = loadCookies()

    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val host = url.host
        val bucket = this.cookies.getOrPut(host) { mutableListOf() }
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
        val host = url.host
        val bucket = cookies[host] ?: return emptyList()
        val now = System.currentTimeMillis()
        val expired = bucket.filter { it.expiresAt <= now }
        if (expired.isNotEmpty()) {
            bucket.removeAll(expired.toSet())
            persist()
        }
        return bucket.filter { it.matches(url) }
    }

    @Synchronized
    fun clear() {
        cookies.clear()
        prefs.edit().clear().apply()
    }

    private fun Cookie.hasExpired(): Boolean = expiresAt <= System.currentTimeMillis()

    private fun loadCookies(): MutableMap<String, MutableList<Cookie>> {
        val map = mutableMapOf<String, MutableList<Cookie>>()
        val raw = prefs.getString(KEY, null) ?: return map
        runCatching {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val cookie = obj.toCookie() ?: continue
                map.getOrPut(cookie.domain) { mutableListOf() }.add(cookie)
            }
        }
        return map
    }

    private fun persist() {
        val arr = JSONArray()
        for ((_, list) in cookies) {
            for (cookie in list) {
                arr.put(cookie.toJson())
            }
        }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    private fun Cookie.toJson(): JSONObject = JSONObject().apply {
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

    private companion object {
        const val PREFS = "typetype_cookies"
        const val KEY = "cookies_json"
    }
}
