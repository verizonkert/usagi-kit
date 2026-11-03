@file:JvmName("ParseUtils")

package tsuki.util

import okhttp3.Response
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import tsuki.InternalParsersApi
import java.text.DateFormat

private val REGEX_SCHEME_PREFIX = Regex("^\\w{2,6}://", RegexOption.IGNORE_CASE)
public const val SCHEME_HTTPS = "https"

/**
 * Parses the [Response] body as a Jsoup HTML [Document], automatically closing the response.
 *
 * @see parseJson
 * @see parseJsonArray
 */
public fun Response.parseHtml(): Document = use { response ->
    val body = response.body
    Jsoup.parse(body.string(), response.request.url.toString())
}

/**
 * Parses the [Response] body as a [JSONObject], automatically closing the response.
 *
 * @see parseJsonArray
 * @see parseHtml
 */
public fun Response.parseJson(): JSONObject = use { response ->
    JSONObject(response.body.string())
}

/**
 * Parses the [Response] body as a [JSONArray], automatically closing the response.
 *
 * @see parseJson
 * @see parseHtml
 */
public fun Response.parseJsonArray(): JSONArray = use { response ->
    JSONArray(response.body.string())
}

/**
 * Reads the [Response] body as a plain UTF-8 String, automatically closing the response.
 */
public fun Response.parseRaw(): String = use { response ->
    response.body.string()
}

/**
 * Reads the [Response] body as a raw [ByteArray], automatically closing the response.
 */
public fun Response.parseBytes(): ByteArray = use { response ->
    response.body.bytes()
}

/**
 * Converts a URL into a relative path string if it belongs to [domain], or leaves it as-is if on an external host.
 *
 * @param domain Target host domain to check against.
 * @return A relative path (starting with `/`) if the URL matches the domain, or the original string otherwise.
 */
public fun String.toRelativeUrl(domain: String): String {
    if (isEmpty() || startsWith("/")) {
        return this
    }
    val cleanDomain = domain.removePrefix("https://").removePrefix("http://").substringBefore('/')
    var stripped = this
    if (stripped.startsWith("https://", ignoreCase = true)) {
        stripped = stripped.substring(8)
    } else if (stripped.startsWith("http://", ignoreCase = true)) {
        stripped = stripped.substring(7)
    } else if (stripped.startsWith("//")) {
        stripped = stripped.substring(2)
    }
    return if (stripped.startsWith(cleanDomain, ignoreCase = true)) {
        val remainder = stripped.substring(cleanDomain.length)
        if (remainder.isEmpty() || remainder.startsWith('/')) {
            if (remainder.isEmpty()) "/" else remainder
        } else {
            this
        }
    } else {
        this
    }
}

/**
 * Converts a relative or protocol-relative URL to an absolute HTTPS URL using [domain].
 *
 * @param domain Host domain to attach if the receiver string is a relative path.
 * @return An absolute URL with an `https` scheme.
 */
public fun String.toAbsoluteUrl(domain: String): String = when {
    startsWith("//") -> "$SCHEME_HTTPS:$this"
    startsWith('/') -> "$SCHEME_HTTPS://$domain$this"
    REGEX_SCHEME_PREFIX.containsMatchIn(this) -> this
    else -> "$SCHEME_HTTPS://$domain/$this"
}

/**
 * Concatenates a host domain and a path ensuring exactly one separating slash.
 */
public fun concatUrl(host: String, path: String): String {
    val hostWithSlash = host.endsWith('/')
    val pathWithSlash = path.startsWith('/')
    val hostWithScheme = if (host.startsWith("//")) "https:$host" else host
    return when {
        hostWithSlash && pathWithSlash -> hostWithScheme + path.drop(1)
        !hostWithSlash && !pathWithSlash -> "$hostWithScheme/$path"
        else -> hostWithScheme + path
    }
}

/**
 * Safely parses a date string into an epoch timestamp in milliseconds, returning `0L` on parse errors.
 */
@InternalParsersApi
public fun DateFormat.parseSafe(str: String?): Long = if (str.isNullOrEmpty()) {
    0L
} else {
    runCatching {
        parse(str)?.time ?: 0L
    }.onFailure {
        if (javaClass.desiredAssertionStatus()) {
            throw AssertionError("Cannot parse date $str", it)
        }
    }.getOrDefault(0L)
}

/**
 * Returns the [ResponseBody] of this response.
 */
@Deprecated("Useless since OkHttp 5.0", replaceWith = ReplaceWith("body"))
public fun Response.requireBody(): ResponseBody = body
