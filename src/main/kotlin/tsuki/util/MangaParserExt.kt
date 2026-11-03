@file:Suppress("warnings", "unused")

package tsuki.util

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * Executes the given [block] lambda if the receiver value is not `null`.
 */
public inline fun <T> T?.ifNotNull(block: (T) -> Unit) {
	if (this != null) block(this)
}

/**
 * Returns the receiver [String] if it is not null and not empty; otherwise returns `null`.
 *
 * Employs a Kotlin contract so that smart-casting to non-null is active when the return value is non-null.
 */
@OptIn(ExperimentalContracts::class)
public inline fun String?.nullIfEmpty(): String? {
	contract {
		returnsNotNull() implies (this@nullIfEmpty != null)
	}
	return if (isNullOrEmpty()) null else this
}
