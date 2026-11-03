package tsuki.util

import kotlinx.coroutines.CancellationException
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Executes the given [block] lambda on the receiver and returns an encapsulated [Result].
 *
 * Unlike standard Kotlin [kotlin.runCatching], this function rethrows [CancellationException]
 * and [InterruptedException] so that Coroutine structured cancellation is not inadvertently swallowed.
 *
 * @param block The block of code to execute.
 * @return A [Result] representing either the successful value or the non-cancellation failure.
 */
@Suppress("WRONG_INVOCATION_KIND") // https://youtrack.jetbrains.com/issue/KT-70714
public inline fun <T, R> T.runCatchingCancellable(block: T.() -> R): Result<R> {
	contract {
		callsInPlace(block, InvocationKind.EXACTLY_ONCE)
	}
	return try {
		Result.success(block())
	} catch (e: InterruptedException) {
		throw e
	} catch (e: CancellationException) {
		throw e
	} catch (e: Throwable) {
		Result.failure(e)
	}
}

/**
 * Recovers from an exception in this [Result] by applying [transform], while properly re-throwing coroutine cancellations.
 */
public inline fun <R, T : R> Result<T>.recoverCatchingCancellable(transform: (exception: Throwable) -> R): Result<R> {
	contract {
		callsInPlace(transform, InvocationKind.AT_MOST_ONCE)
	}
	return when (val exception = exceptionOrNull()) {
		null -> this
		else -> runCatchingCancellable { transform(exception) }
	}
}

/**
 * Recovers from a failure if [transform] produces a non-null fallback value, otherwise retaining the failure.
 */
public inline fun <R : Any, T : R> Result<T>.recoverNotNull(transform: (exception: Throwable) -> R?): Result<R> {
	contract {
		callsInPlace(transform, InvocationKind.AT_MOST_ONCE)
	}
	val exception = exceptionOrNull() ?: return this
	return try {
		transform(exception)?.let { Result.success(it) } ?: this
	} catch (e: InterruptedException) {
		throw e
	} catch (e: CancellationException) {
		throw e
	} catch (e: Throwable) {
		Result.failure(e)
	}
}
