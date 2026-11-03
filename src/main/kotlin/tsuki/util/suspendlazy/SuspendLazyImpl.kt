package tsuki.util.suspendlazy

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Standard thread-safe, coroutine-safe implementation of [SuspendLazy].
 *
 * Employs double-checked locking using a coroutine [Mutex]. Once initialized,
 * the [initializer] reference is released to avoid holding memory references.
 *
 * @param coroutineContext Optional context to run the initializer in.
 * @param initializer The suspending computation lambda.
 */
public class SuspendLazyImpl<T>(
	private val coroutineContext: CoroutineContext,
	initializer: SuspendLazyInitializer<T>,
) : SuspendLazy<T> {

	private val mutex: Mutex = Mutex()
	private var initializer: SuspendLazyInitializer<T>? = initializer
	@Volatile
	private var cachedValue: Any? = Uninitialized

	override val isInitialized: Boolean
		get() = cachedValue !== Uninitialized

	@Suppress("UNCHECKED_CAST")
	override suspend fun get(): T {
		// fast way
		val current = cachedValue
		if (current !== Uninitialized) {
			return current as T
		}
		return mutex.withLock {
			val lockCurrent = cachedValue
			if (lockCurrent !== Uninitialized) {
				return lockCurrent as T
			}
			val init = initializer ?: error("Initializer has already been cleared")
			val result = if (coroutineContext === EmptyCoroutineContext) {
				init()
			} else {
				withContext(coroutineContext) {
					init()
				}
			}
			cachedValue = result
			initializer = null // Prevent retaining captured outer references
			result
		}
	}

	@Suppress("UNCHECKED_CAST")
	override fun peek(): T? {
		return cachedValue?.takeUnless { it === Uninitialized } as T?
	}

	/**
	 * Sentinel token denoting that the lazy value has not yet been computed.
	 */
	private object Uninitialized
}
