@file:JvmName("StringUtils")
@file:Suppress("unused")

package tsuki.util

import androidx.collection.MutableIntList
import java.math.BigInteger
import java.net.URLDecoder
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.Locale
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.math.min

private val REGEX_WHITESPACE = Regex("\\s+")

/**
 * 64-bit polynomial hash seed constant used for generating deterministic identifiers.
 */
public const val LONG_HASH_SEED = 1125899906842597L

/**
 * Strips matched prefix and suffix characters from this string if both match one of [chars].
 */
public fun String.removeSurrounding(vararg chars: Char): String {
	if (isEmpty()) {
		return this
	}
	for (c in chars) {
		if (first() == c && last() == c) {
			return substring(1, length - 1)
		}
	}
	return this
}

/**
 * Returns [defaultValue] if this CharSequence is `null` or empty, otherwise returns `this`.
 */
public inline fun <C : R, R : CharSequence?> C?.ifNullOrEmpty(defaultValue: () -> R): R {
	contract {
		callsInPlace(defaultValue, InvocationKind.AT_MOST_ONCE)
	}
	return if (this.isNullOrEmpty()) defaultValue() else this
}

/**
 * Computes a 64-bit polynomial hash code seeded by [LONG_HASH_SEED].
 */
public fun String.longHashCode(): Long {
	var h = LONG_HASH_SEED
	val len: Int = this.length
	for (i in 0 until len) {
		h = 31 * h + this[i].code
	}
	return h
}

/**
 * Converts whitespace-separated words in this string to CamelCase representation.
 */
public fun String.toCamelCase(): String {
	if (isEmpty()) {
		return this
	}
	val result = StringBuilder(length)
	var capitalize = true
	for (char in this) {
		result.append(
			if (capitalize) {
				char.uppercase()
			} else {
				char.lowercase()
			},
		)
		capitalize = char.isWhitespace()
	}
	return result.toString()
}

/**
 * Extracts only digit characters (`0-9`) from this string.
 */
public fun String.digits(): String = filter { it.isDigit() }

/**
 * Capitalizes the first character of this string using the default locale.
 */
public fun String.toTitleCase(): String {
	return replaceFirstChar { x -> x.uppercase() }
}

/**
 * Capitalizes the first character of this string using the specified [locale].
 */
public fun String.toTitleCase(locale: Locale): String {
	return replaceFirstChar { x -> x.uppercase(locale) }
}

/**
 * Truncates this string to [maxLength] and appends an ellipsis character (`…`) if it exceeds the limit.
 */
public fun String.ellipsize(maxLength: Int): String = if (this.length > maxLength) {
	this.take(maxLength - 1) + Typography.ellipsis
} else this

/**
 * Splits this string into two parts at the middle occurrence of [delimiter].
 */
public fun String.splitTwoParts(delimiter: Char): Pair<String, String>? {
	val indices = MutableIntList(4)
	for ((i, c) in this.withIndex()) {
		if (c == delimiter) {
			indices += i
		}
	}
	if (indices.isEmpty() || (indices.size and 1) == 0) {
		return null
	}
	val index = indices[indices.size / 2]
	return substring(0, index) to substring(index + 1)
}

/**
 * Encodes this string using UTF-8 URL encoding rules.
 */
public fun String?.urlEncoded(): String = URLEncoder.encode(this ?: "", Charsets.UTF_8.name())

/**
 * Decodes this URL-encoded string using UTF-8.
 */
public fun String.urlDecode(): String = URLDecoder.decode(this, Charsets.UTF_8.name())

/**
 * Converts newline characters (`\n`) to HTML `<br>` tags.
 */
public fun String.nl2br(): String = replace("\n", "<br>")

/**
 * Splits this string by one or more contiguous whitespace characters into a list of words.
 */
public fun String?.splitByWhitespace(): List<String> = this?.trim()?.split(REGEX_WHITESPACE) ?: emptyList()

/**
 * Returns `null` if this CharSequence is empty, otherwise returns `this`.
 */
public fun <T : CharSequence> T.nullIfEmpty(): T? = takeUnless { it.isEmpty() }

/**
 * Formats this byte array as a colon-separated uppercase hexadecimal string (e.g. `"0A:FF:2C"`).
 */
public fun ByteArray.byte2HexFormatted(): String {
	val str = StringBuilder(size * 2)
	for (i in indices) {
		var h = Integer.toHexString(this[i].toInt())
		val l = h.length
		if (l == 1) {
			h = "0$h"
		}
		if (l > 2) {
			h = h.substring(l - 2, l)
		}
		str.append(h.uppercase(Locale.ROOT))
		if (i < size - 1) {
			str.append(':')
		}
	}
	return str.toString()
}

/**
 * Computes a 32-character lowercase MD5 hex digest string from this string's UTF-8 bytes.
 */
public fun String.md5(): String {
	val md = MessageDigest.getInstance("MD5")
	return BigInteger(1, md.digest(toByteArray()))
		.toString(16)
		.padStart(32, '0')
}

/**
 * Extracts the substring bounded between the first occurrence of [from] and the last occurrence of [to].
 */
public fun String.substringBetween(from: String, to: String, fallbackValue: String = this): String {
	val fromIndex = indexOf(from)
	if (fromIndex == -1) {
		return fallbackValue
	}
	val toIndex = lastIndexOf(to)
	return if (toIndex == -1) {
		fallbackValue
	} else {
		substring(fromIndex + from.length, toIndex)
	}
}

/**
 * Extracts the substring bounded between the first occurrence of [from] and the subsequent first occurrence of [to].
 */
public fun String.substringBetweenFirst(from: String, to: String): String? {
	val fromIndex = indexOf(from)
	if (fromIndex == -1) {
		return null
	}
	val toIndex = indexOf(to, fromIndex)
	return if (toIndex == -1) {
		null
	} else {
		substring(fromIndex + from.length, toIndex)
	}
}

/**
 * Extracts the substring bounded between the last occurrence of [from] and the subsequent last occurrence of [to].
 */
public fun String.substringBetweenLast(from: String, to: String, fallbackValue: String = this): String {
	val fromIndex = lastIndexOf(from)
	if (fromIndex == -1) {
		return fallbackValue
	}
	val toIndex = lastIndexOf(to)
	return if (toIndex == -1) {
		fallbackValue
	} else {
		substring(fromIndex + from.length, toIndex)
	}
}

/**
 * Returns the first substring matching [regex], or `null` if no match exists.
 */
public fun String.find(regex: Regex): String? = regex.find(this)?.value

/**
 * Returns the first captured group (group index 1) matching [regex], or `null` if not matched.
 */
public fun String.findGroupValue(regex: Regex): String? = regex.find(this)?.groupValues?.getOrNull(1)

/**
 * Removes the trailing character [suffix] if present.
 */
public fun String.removeSuffix(suffix: Char): String {
	if (lastOrNull() == suffix) {
		return substring(0, length - 1)
	}
	return this
}

/**
 * Calculates the Levenshtein edit distance between this string and [other].
 */
public fun String.levenshteinDistance(other: String): Int {
	if (this == other) {
		return 0
	}
	if (this.isEmpty()) {
		return other.length
	}
	if (other.isEmpty()) {
		return this.length
	}

	val lhsLength = this.length + 1
	val rhsLength = other.length + 1

	var cost = IntArray(lhsLength) { it }
	var newCost = IntArray(lhsLength) { 0 }

	for (i in 1 until rhsLength) {
		newCost[0] = i

		for (j in 1 until lhsLength) {
			val match = if (this[j - 1] == other[i - 1]) 0 else 1

			val costReplace = cost[j - 1] + match
			val costInsert = cost[j] + 1
			val costDelete = newCost[j - 1] + 1

			newCost[j] = min(min(costInsert, costDelete), costReplace)
		}

		val swap = cost
		cost = newCost
		newCost = swap
	}

	return cost[lhsLength - 1]
}

/**
 * Determines whether this string is approximately equal to [other] within a normalized distance [threshold].
 *
 * @param threshold Allowed edit distance ratio (where `0.0` is exact match, and larger values allow more difference).
 */
public fun String.almostEquals(other: String, threshold: Float): Boolean {
	if (threshold <= 0f) {
		return equals(other, ignoreCase = true)
	}
	val diff = lowercase().levenshteinDistance(other.lowercase()) / ((length + other.length) / 2f)
	return diff < threshold
}

/**
 * Checks whether all characters in this string are decimal digits.
 */
public fun String.isNumeric(): Boolean = all { c -> c.isDigit() }

/**
 * Strips redundant trailing decimal zeroes (e.g. `".0"` or `",0"`) from this [StringBuilder].
 */
public fun StringBuilder.removeTrailingZero() {
	if (length > 2 && get(length - 1) == '0') {
		val dot = get(length - 2)
		if (dot == ',' || dot == '.') {
			delete(length - 2, length)
		}
	}
}

/**
 * Generates a random alphanumeric string of length [length] sampled from [pool].
 */
public fun String.Companion.random(
	length: Int,
	pool: String = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789",
): String = CharArray(length) { pool.random() }.concatToString()
