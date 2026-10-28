package org.draken.usagikit.context

import android.webkit.CookieManager
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.util.concurrent.ConcurrentHashMap

/**
 * Android WebKit backed CookieJar supporting in-memory and WebKit cookies.
 */
class UsagiCookieJar : CookieJar {

    private val cookieStore = ConcurrentHashMap<String, MutableList<Cookie>>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val host = url.host
        val current = cookieStore.getOrPut(host) { mutableListOf() }
        synchronized(current) {
            cookies.forEach { cookie ->
                current.removeAll { it.name == cookie.name }
                current.add(cookie)
            }
        }

        runCatching {
            val manager = CookieManager.getInstance()
            val urlStr = url.toString()
            cookies.forEach { cookie ->
                manager.setCookie(urlStr, cookie.toString())
            }
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val host = url.host
        val memoryCookies = cookieStore[host].orEmpty().filter { !it.hasExpired() }

        val webkitCookies = runCatching {
            val manager = CookieManager.getInstance()
            val raw = manager.getCookie(url.toString()) ?: return@runCatching emptyList()
            raw.split(";").mapNotNull { Cookie.parse(url, it.trim()) }
        }.getOrDefault(emptyList())

        val combined = LinkedHashMap<String, Cookie>()
        memoryCookies.forEach { combined[it.name] = it }
        webkitCookies.forEach { combined[it.name] = it }

        return combined.values.toList()
    }

    fun getCookies(domain: String): List<Cookie> {
        val httpUrl = HttpUrl.Builder().scheme("https").host(domain).build()
        return loadForRequest(httpUrl)
    }

    fun insertCookie(domain: String, cookie: Cookie) {
        val httpUrl = HttpUrl.Builder().scheme("https").host(domain).build()
        saveFromResponse(httpUrl, listOf(cookie))
    }

    fun insertCookies(domain: String, vararg cookieStrings: String) {
        val httpUrl = HttpUrl.Builder().scheme("https").host(domain).build()
        val parsed = cookieStrings.mapNotNull { Cookie.parse(httpUrl, it) }
        saveFromResponse(httpUrl, parsed)
    }

    fun copyCookies(fromDomain: String, toDomain: String) {
        val fromCookies = getCookies(fromDomain)
        val toUrl = HttpUrl.Builder().scheme("https").host(toDomain).build()
        val copied = fromCookies.map { c ->
            c.newBuilder().domain(toDomain).build()
        }
        saveFromResponse(toUrl, copied)
    }

    private fun Cookie.hasExpired(): Boolean {
        return expiresAt < System.currentTimeMillis()
    }
}
