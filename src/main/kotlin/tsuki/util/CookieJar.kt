@file:JvmName("CookieJarUtils")

package tsuki.util

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

/**
 * Parses and inserts raw `Set-Cookie` string headers for the specified [domain] into this [CookieJar].
 *
 * @param domain Target host domain (e.g. `"example.com"` or `"https://example.com"`).
 * @param cookies Raw `Set-Cookie` string header values.
 */
public fun CookieJar.insertCookies(domain: String, vararg cookies: String) {
	val url = safeUrlOf(domain) ?: return
	saveFromResponse(
		url,
		cookies.mapNotNull {
			Cookie.parse(url, it)
		},
	)
}

/**
 * Inserts a single pre-built [Cookie] into this [CookieJar] for the specified [domain].
 *
 * @param domain Target host domain (e.g. `"example.com"`).
 * @param cookie The [Cookie] instance to associate with the domain.
 */
public fun CookieJar.insertCookie(domain: String, cookie: Cookie) {
	val url = safeUrlOf(domain) ?: return
	saveFromResponse(url, listOf(cookie))
}

/**
 * Retrieves all stored cookies matching the given [domain].
 *
 * @param domain Target host domain to query.
 * @return List of matching [Cookie] instances, or an empty list if domain is invalid or has no cookies.
 */
public fun CookieJar.getCookies(domain: String): List<Cookie> {
	val url = safeUrlOf(domain) ?: return emptyList()
	return loadForRequest(url)
}

/**
 * Copies stored cookies from [oldDomain] to [newDomain] (e.g., when a manga source migrates to a mirror domain).
 *
 * @param oldDomain Origin domain to read cookies from.
 * @param newDomain Destination domain to replicate cookies to with an updated domain attribute.
 * @param names Optional whitelist filter; only cookies with names matching this array are copied.
 */
public fun CookieJar.copyCookies(oldDomain: String, newDomain: String, names: Array<String>? = null) {
	val oldUrl = safeUrlOf(oldDomain) ?: return
	val newUrl = safeUrlOf(newDomain) ?: return
	var cookies = loadForRequest(oldUrl)
	if (names != null) {
		val nameSet = names.toHashSet()
		cookies = cookies.filter { c -> c.name in nameSet }
	}
	val cleanNewHost = newUrl.host
	val newCookies = cookies.map { cookie ->
		cookie.newBuilder().domain(cleanNewHost).build()
	}
	saveFromResponse(newUrl, newCookies)
}

/**
 * Safely creates an [HttpUrl] instance from a loose domain or URL string for cookie operations.
 */
private fun safeUrlOf(domain: String): HttpUrl? = try {
	val cleanDomain = domain.removePrefix("https://").removePrefix("http://").substringBefore('/')
	HttpUrl.Builder()
		.scheme(SCHEME_HTTPS)
		.host(cleanDomain)
		.build()
} catch (_: IllegalArgumentException) {
	null
}
