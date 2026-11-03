@file:JvmName("EnumUtils")

package tsuki.util

import kotlin.enums.EnumEntries

/**
 * Returns an array containing the exact [Enum.name] string of each entry in this [EnumEntries].
 *
 * Pre-allocates an array of the exact required size for optimal performance.
 *
 * @return An array of enum entry names.
 */
public fun <E : Enum<E>> EnumEntries<E>.names(): Array<String> = Array(size) { i ->
	get(i).name
}

/**
 * Finds the first enum entry matching the specified [name], ignoring case sensitivity.
 *
 * @param name The identifier to search for.
 * @return Matching enum constant [E], or `null` if no match was found.
 */
public fun <E : Enum<E>> EnumEntries<E>.find(name: String): E? {
	return firstOrNull { it.name.equals(name, ignoreCase = true) }
}
