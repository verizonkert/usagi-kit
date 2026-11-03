package tsuki.network

import okhttp3.CookieJar
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Response
import org.jsoup.Jsoup
import java.net.HttpURLConnection.HTTP_FORBIDDEN
import java.net.HttpURLConnection.HTTP_UNAVAILABLE

/**
 * Utility helper for detecting and handling Cloudflare bot protection mechanisms.
 *
 * Inspects HTTP error responses (HTTP 403 Forbidden and HTTP 503 Service Unavailable)
 * for Cloudflare challenge patterns (Turnstile, interactive CAPTCHAs, or IP blocks),
 * and provides helpers to inspect clearance cookies.
 */
public object CloudFlareHelper {

    /** Indicates that no Cloudflare challenge or block was identified in the response. */
    public const val PROTECTION_NOT_DETECTED: Int = 0
    /** Indicates that Cloudflare presented an interactive challenge, Turnstile, or CAPTCHA page. */
    public const val PROTECTION_CAPTCHA: Int = 1
    /** Indicates that the client IP was outright blocked by Cloudflare firewall rules. */
    public const val PROTECTION_BLOCKED: Int = 2

    private const val CF_CLEARANCE = "cf_clearance"

    /**
     * Inspects the given HTTP [response] for Cloudflare challenge and block signatures.
     *
     * Peeks at the HTML response body (without consuming the original stream) and checks for
     * known Cloudflare Turnstile containers, challenge forms, and error titles.
     *
     * @param response The intercepted HTTP [Response].
     * @return One of [PROTECTION_NOT_DETECTED], [PROTECTION_CAPTCHA], or [PROTECTION_BLOCKED].
     */
    public fun checkResponseForProtection(response: Response): Int {
        if (response.code != HTTP_FORBIDDEN && response.code != HTTP_UNAVAILABLE) {
            return PROTECTION_NOT_DETECTED
        }
        val content = try {
            response.peekBody(Long.MAX_VALUE).use {
                Jsoup.parse(it.byteStream(), Charsets.UTF_8.name(), response.request.url.toString())
            }
        } catch (_: IllegalStateException) {
            return PROTECTION_NOT_DETECTED
        }
        return when {
            content.selectFirst("h2[data-translate=\"blocked_why_headline\"]") != null -> PROTECTION_BLOCKED
			content.getElementById("challenge-error-title") != null ||
				content.getElementById("challenge-error-text") != null ||
				content.selectFirst("form#challenge-form") != null ||
				content.selectFirst("iframe[src*='challenges.cloudflare.com']") != null ||
				content.selectFirst("div#turnstile-wrapper") != null ||
				content.selectFirst("div.cf-turnstile") != null ||
				content.getElementById("cf-wrapper") != null ||
				content.title() == "Just a moment..." ||
				content.title() == "Attention Required! | Cloudflare" ||
				content.body().text().let { text ->
					text.contains("Verify you are human") ||
					text.contains("needs to review the security of your connection")
				} -> PROTECTION_CAPTCHA
			else -> PROTECTION_NOT_DETECTED
        }
    }

    /**
     * Extracts the value of the `cf_clearance` cookie from the given [cookieJar] for [url].
     *
     * @param cookieJar The [CookieJar] instance storing session cookies.
     * @param url The target website URL string.
     * @return The `cf_clearance` cookie value, or `null` if not present.
     */
    public fun getClearanceCookie(cookieJar: CookieJar, url: String): String? {
        return cookieJar.loadForRequest(url.toHttpUrl()).find { it.name == CF_CLEARANCE }?.value
    }

    /**
     * Checks whether the given cookie [name] belongs to Cloudflare or related CSRF tokens.
     *
     * @param name The cookie name to test.
     * @return `true` if the cookie is associated with Cloudflare or CSRF verification.
     */
    public fun isCloudFlareCookie(name: String): Boolean {
        return name.startsWith("cf_")
            || name.startsWith("_cf")
            || name.startsWith("__cf")
            || name == "csrftoken"
    }
}

