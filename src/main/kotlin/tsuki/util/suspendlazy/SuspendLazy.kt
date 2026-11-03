package tsuki.util.suspendlazy

import tsuki.util.runCatchingCancellable
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Suspending initializer lambda that produces a value of type [T].
 */
public typealias SuspendLazyInitializer<T> = suspend () -> T

/**
 * Represents a value whose computation is deferred until first requested via [get],
 * evaluated asynchronously using Kotlin Coroutines, and cached for subsequent accesses.
 *
 * Thread-safe and coroutine-safe via double-checked locking using a coroutine [kotlinx.coroutines.sync.Mutex].
 *
 * @param T The type of the lazily computed value.
 */
public interface SuspendLazy<T> {

	/**
	 * Returns `true` if the lazy value has already been computed and is currently cached in memory.
	 */
	public val isInitialized: Boolean

	/**
	 * Returns the cached value if already computed, or invokes the suspending initializer to compute and cache it.
	 *
	 * Thread-safe: concurrent calls will suspend and share the single initializer computation.
	 *
	 * @return The lazily computed value.
	 */
	public suspend fun get(): T

	/**
	 * Returns the currently cached value if already computed, or `null` if not yet initialized
	 * (or if garbage collected in soft reference variants), without triggering initialization.
	 */
	public fun peek(): T?
}

/**
 * Awaits the value of this [SuspendLazy], returning `null` if the initializer throws an exception.
 *
 * Re-throws coroutine cancellations to respect structured concurrency.
 */
public suspend fun <T> SuspendLazy<T>.getOrNull(): T? {
	peek()?.let { return it }
	return runCatchingCancellable {
		get()
	}.getOrNull()
}

/**
 * Awaits the value of this [SuspendLazy], returning [defaultValue] if the initializer fails.
 */
public suspend fun <R, T : R> SuspendLazy<T>.getOrDefault(defaultValue: R): R {
	peek()?.let { return it }
	return runCatchingCancellable {
		get()
	}.getOrDefault(defaultValue)
}

/**
 * Awaits the value of this [SuspendLazy], invoking [defaultValue] with the exception if initialization fails.
 */
public suspend inline fun <T> SuspendLazy<T>.getOrElse(defaultValue: (Throwable) -> T): T {
	peek()?.let { return it }
	return runCatchingCancellable {
		get()
	}.getOrElse(defaultValue)
}

/**
 * Creates a new [SuspendLazy] instance that computes its value using [initializer] on first access.
 *
 * @param context Optional [CoroutineContext] to switch to during initialization (e.g. `Dispatchers.IO`).
 * @param initializer The suspending computation lambda.
 */
public fun <T> suspendLazy(
	context: CoroutineContext = EmptyCoroutineContext,
	initializer: SuspendLazyInitializer<T>,
): SuspendLazy<T> = SuspendLazyImpl(context, initializer)

/**
 * Creates a new [SuspendLazy] instance, optionally backed by a [java.lang.ref.SoftReference] cache.
 *
 * @param context Optional [CoroutineContext] to switch to during initialization.
 * @param soft If `true`, the cached value may be cleared by the Garbage Collector under memory pressure
 *             and re-computed on subsequent access.
 * @param initializer The suspending computation lambda.
 */
public fun <T : Any> suspendLazy(
	context: CoroutineContext = EmptyCoroutineContext,
	soft: Boolean,
	initializer: SuspendLazyInitializer<T>,
): SuspendLazy<T> = if (soft) {
	SoftSuspendLazyImpl(context, initializer)
} else {
	SuspendLazyImpl(context, initializer)
}
