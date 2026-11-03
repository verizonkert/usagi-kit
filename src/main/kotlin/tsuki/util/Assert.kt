package tsuki.util

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * Asserts that the receiver value is not `null`, throwing an [AssertionError] if it is.
 *
 * Utilizes Kotlin contracts to smart-cast the receiver to a non-null type [T]
 * for the remaining scope after the call.
 *
 * @param name The descriptive identifier or variable name to include in the error message if null.
 * @return The non-null receiver value of type [T].
 * @throws AssertionError if the receiver value is `null`.
 */
@OptIn(ExperimentalContracts::class)
public inline fun <T : Any> T?.assertNotNull(name: String): T {
	contract {
		returns() implies (this@assertNotNull != null)
	}
	if (this == null) {
		throw AssertionError("Value $name is null")
	}
	return this
}

