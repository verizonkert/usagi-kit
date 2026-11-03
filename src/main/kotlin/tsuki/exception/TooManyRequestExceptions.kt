package tsuki.exception

import okio.IOException
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Thrown when an HTTP request exceeds remote rate limits (e.g. HTTP 429 Too Many Requests).
 *
 * Provides the target [url] and optional [retryAt] timestamp indicating when the caller may retry.
 *
 * @property url The target endpoint URL that rejected the request.
 * @param retryAfter Delay duration in milliseconds after which the request may be retried.
 */
public class TooManyRequestExceptions(
	public val url: String,
	retryAfter: Long,
) : IOException("Too many requests") {

	/**
	 * Specific point in time when the rate limit window expires and the request can be retried.
	 */
	public val retryAt: Instant? = if (retryAfter > 0 && retryAfter < Long.MAX_VALUE / 2) {
		runCatching { Instant.now().plusMillis(retryAfter) }.getOrNull()
	} else {
		null
	}

	/**
	 * Returns the remaining time in milliseconds before the request can be retried, or `-1L` if unknown.
	 */
	public fun getRetryDelay(): Long {
		val at = retryAt ?: return -1L
		return Instant.now().until(at, ChronoUnit.MILLIS).coerceAtLeast(0L)
	}

	override val message: String?
		get() = if (retryAt != null) {
			"${super.message}, retry at $retryAt"
		} else {
			super.message
		}
}
