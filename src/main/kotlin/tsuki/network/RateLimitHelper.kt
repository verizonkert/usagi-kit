package tsuki.network

import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import tsuki.network.utils.RateLimiter
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration

/**
 * OkHttp [Interceptor] that enforces client-side rate limits on outbound HTTP requests per host.
 *
 * Uses a sliding-window [RateLimiter] per host to ensure parsers do not exceed the source's
 * request frequency limits (e.g. avoiding HTTP 429 errors or IP bans).
 *
 * @param permits Maximum allowed requests per host within the rolling [period].
 * @param period Time duration of the rolling window.
 * @param shouldLimit Filter predicate determining whether a given [HttpUrl] should be subject to rate limiting.
 */
public class RateLimitHelper(
	private val permits: Int,
	private val period: Duration,
	private val shouldLimit: (HttpUrl) -> Boolean = { true }
) : Interceptor {

	/**
	 * Convenience constructor rate limiting all requests intercepted by this helper.
	 *
	 * @param permits Maximum allowed requests per host within the rolling [period].
	 * @param period Time duration of the rolling window.
	 */
	public constructor(permits: Int, period: Duration) : this(permits, period, { true })

	private val limiters = ConcurrentHashMap<String, RateLimiter>()

	/**
	 * Intercepts the request, acquires a permit from the host's [RateLimiter], executes the request,
	 * and safely releases the permit if the call was canceled, failed, or served from cache.
	 */
	override fun intercept(chain: Interceptor.Chain): Response {
		val request = chain.request()

		if (!shouldLimit(request.url)) {
			return chain.proceed(request)
		}

		val host = request.url.host
		val limiter = limiters.computeIfAbsent(host) {
			RateLimiter(permits, period.inWholeMilliseconds)
		}

		val call = chain.call()
		val timestamp = limiter.acquire(
			call = call,
			url = request.url.toString(),
		)

		val response: Response
		try {
			if (call.isCanceled()) {
				limiter.release(timestamp)
				throw IOException("Canceled")
			}
			response = chain.proceed(request)
		} catch (e: Exception) {
			limiter.release(timestamp)
			throw e
		}

		if (response.networkResponse == null) {
			limiter.release(timestamp)
		}

		return response
	}
}
