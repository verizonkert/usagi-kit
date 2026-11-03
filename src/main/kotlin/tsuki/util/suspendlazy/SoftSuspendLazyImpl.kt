package tsuki.util.suspendlazy

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.lang.ref.SoftReference
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Memory-sensitive implementation of [SuspendLazy] backed by a [SoftReference].
 *
 * The computed value is retained in memory as long as memory is sufficient. Under GC pressure,
 * the JVM may clear the soft reference, in which case a subsequent [get] will re-run the [initializer].
 *
 * @param coroutineContext Optional context to run the initializer in.
 * @param initializer The suspending computation lambda.
 */
public class SoftSuspendLazyImpl<T : Any>(
	private val coroutineContext: CoroutineContext,
	private val initializer: SuspendLazyInitializer<T>,
) : SuspendLazy<T> {

	private val mutex: Mutex = Mutex()
	@Volatile
	private var cachedValue: SoftReference<T>? = null

	override val isInitialized: Boolean
		get() = cachedValue?.get() != null

	override suspend fun get(): T {
		// fast way
		cachedValue?.get()?.let {
			return it
		}
		return mutex.withLock {
			cachedValue?.get()?.let {
				return it
			}
			val result = if (coroutineContext === EmptyCoroutineContext) {
				initializer()
			} else {
				withContext(coroutineContext) {
					initializer()
				}
			}
			cachedValue = SoftReference(result)
			result
		}
	}

	override fun peek(): T? = cachedValue?.get()
}
