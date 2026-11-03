package tsuki.network.utils

import okhttp3.Call
import tsuki.exception.TooManyRequestExceptions
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Thread-safe sliding-window rate limiter designed for HTTP calls.
 *
 * Restricts outgoing requests so that at most [permits] requests occur within any rolling
 * [periodMs] window. When requests exceed the threshold, [acquire] throws a [TooManyRequestExceptions]
 * indicating how long the client must wait.
 *
 * @param permits Maximum number of requests allowed within the given [periodMs] window. Must be > 0.
 * @param periodMs Time period window in milliseconds. Must be > 0.
 */
public class RateLimiter(
	private val permits: Int,
	private val periodMs: Long
) {
	init {
		require(permits > 0) { "permits must be > 0: $permits" }
		require(periodMs > 0) { "periodMs must be > 0: $periodMs" }
	}

	private val periodNanos = TimeUnit.MILLISECONDS.toNanos(periodMs)
	private val timestamps = ArrayDeque<Long>(permits)
	private val lock = ReentrantLock()

	/**
	 * Attempts to acquire an execution permit for the given HTTP [call].
	 *
	 * Purges expired timestamps from the sliding window. If the number of permits within
	 * the current window has reached capacity, throws [TooManyRequestExceptions] containing
	 * the remaining delay required before a new permit can be granted.
	 *
	 * @param call The OkHttp [Call] being checked.
	 * @param url Optional target URL string for diagnostic and error reporting purposes.
	 * @return The acquisition timestamp in nanoseconds, suitable for passing to [release] if the call fails or cancels.
	 * @throws IOException If the [call] was already canceled prior to acquisition.
	 * @throws TooManyRequestExceptions If the rate limit has been exceeded.
	 */
	public fun acquire(call: Call, url: String = ""): Long = lock.withLock {
		if (call.isCanceled()) {
			throw IOException("Canceled")
		}

		val now = System.nanoTime()

		while (timestamps.isNotEmpty() && now - timestamps.first() >= periodNanos) {
			timestamps.removeFirst()
		}

		if (timestamps.size >= permits) {
			val oldestRequest = timestamps.first()
			val waitNanos = periodNanos - (now - oldestRequest)

			if (waitNanos > 0) {
				val waitMs = TimeUnit.NANOSECONDS.toMillis(waitNanos).coerceAtLeast(1L)
				throw TooManyRequestExceptions(url, waitMs)
			}
		}

		timestamps.addLast(now)
		return now
	}

	/**
	 * Releases a previously acquired permit associated with [timestamp].
	 *
	 * Useful when a request is canceled, fails with an exception, or is served from cache
	 * without performing an actual network round-trip.
	 *
	 * @param timestamp The nanosecond timestamp returned by [acquire].
	 * @return `true` if the timestamp was found and removed, `false` otherwise.
	 */
	public fun release(timestamp: Long): Boolean = lock.withLock {
		val index = timestamps.indexOf(timestamp)
		if (index != -1) {
			timestamps.removeAt(index)
			true
		} else {
			false
		}
	}
}

