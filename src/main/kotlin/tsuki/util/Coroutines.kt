package tsuki.util

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/**
 * Cancels all coroutine [Job] instances in this iterable with the specified optional [cause].
 */
public fun Iterable<Job>.cancelAll(cause: CancellationException? = null) {
	forEach { it.cancel(cause) }
}

/**
 * Awaits the first [Deferred] task in this collection to complete successfully, cancelling all remaining tasks.
 *
 * Useful for concurrent racing (e.g. querying multiple CDN mirror servers or fallback domains simultaneously).
 * Exceptions from failing candidates are suppressed until at least one succeeds.
 *
 * @return The result of the first task that successfully completed.
 * @throws NoSuchElementException if all deferred operations fail without producing a result.
 */
public suspend fun <T> Iterable<Deferred<T>>.awaitFirst(): T {
	val list = if (this is Collection<Deferred<T>>) this else toList()
	return try {
		channelFlow<T> {
			for (deferred in list) {
				launch {
					try {
						send(deferred.await())
					} catch (e: CancellationException) {
						throw e
					} catch (_: Throwable) {
						// Allow other concurrent mirrors / deferreds to succeed
					}
				}
			}
		}.first()
	} finally {
		list.cancelAll()
	}
}

/**
 * Awaits the first [Deferred] task whose result satisfies the given [condition], cancelling all remaining tasks.
 *
 * @param condition Predicate that the result must fulfill to be selected.
 * @return The result of the first task that completed and satisfied [condition].
 * @throws NoSuchElementException if no task succeeds and meets the condition.
 */
public suspend fun <T> Collection<Deferred<T>>.awaitFirst(condition: (T) -> Boolean): T {
	return try {
		channelFlow {
			for (deferred in this@awaitFirst) {
				launch {
					try {
						val result = deferred.await()
						if (condition(result)) {
							send(result)
						}
					} catch (e: CancellationException) {
						throw e
					} catch (_: Throwable) {
						// Allow other concurrent mirrors / deferreds to succeed
					}
				}
			}
		}.first()
	} finally {
		this.cancelAll()
	}
}
