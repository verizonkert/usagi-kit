@file:JvmName("NumberUtils")

package tsuki.util

import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.math.absoluteValue

/**
 * Formats this [Number] into a localized or custom numeric string.
 *
 * @param decimals Number of decimal digits to retain.
 * @param decPoint Decimal separator character (defaults to `'.'`).
 * @param thousandsSep Optional grouping thousands separator (e.g. `' '` or `','`), or `null` to disable grouping.
 * @return Formatted number string.
 */
public fun Number.format(decimals: Int = 0, decPoint: Char = '.', thousandsSep: Char? = ' '): String {
	val formatter = NumberFormat.getInstance(Locale.US) as DecimalFormat
	val symbols = formatter.decimalFormatSymbols
	if (thousandsSep != null) {
		symbols.groupingSeparator = thousandsSep
		formatter.isGroupingUsed = true
	} else {
		formatter.isGroupingUsed = false
	}
	symbols.decimalSeparator = decPoint
	formatter.decimalFormatSymbols = symbols
	formatter.minimumFractionDigits = decimals
	formatter.maximumFractionDigits = decimals
	return when (this) {
		is Float,
		is Double,
			-> formatter.format(this.toDouble())

		else -> formatter.format(this.toLong())
	}
}

/**
 * Rounds a floating-point number upwards to the nearest integer with a small floating-point tolerance.
 */
public fun Float.toIntUp(): Int {
	if (this <= 0f) return 0
	val intValue = toInt()
	return if ((this - intValue.toFloat()).absoluteValue <= 0.00001f) {
		intValue
	} else {
		intValue + 1
	}
}

/**
 * Rounds this integer up to the next multiple of [step].
 */
public infix fun Int.upBy(step: Int): Int {
	val mod = this % step
	return if (mod == 0) {
		this
	} else {
		this - mod + step
	}
}

/**
 * Formats a number cleanly, stripping redundant trailing `".0"` or `",0"`.
 */
public fun Number.formatSimple(): String {
	val raw = toString()
	return if (raw.endsWith(".0") || raw.endsWith(",0")) {
		raw.dropLast(2)
	} else {
		raw
	}
}

/**
 * Returns [defaultValue] if this integer is `0`, otherwise returns `this`.
 */
public inline fun Int.ifZero(defaultValue: () -> Int): Int {
	contract {
		callsInPlace(defaultValue, InvocationKind.AT_MOST_ONCE)
	}
	return if (this == 0) {
		defaultValue()
	} else {
		this
	}
}

/**
 * Returns [defaultValue] if this long is `0L`, otherwise returns `this`.
 */
public inline fun Long.ifZero(defaultValue: () -> Long): Long {
	contract {
		callsInPlace(defaultValue, InvocationKind.AT_MOST_ONCE)
	}
	return if (this == 0L) {
		defaultValue()
	} else {
		this
	}
}

/**
 * Combines two 32-bit integers into a single 64-bit Long (high 32 bits from [a], low 32 bits from [b]).
 */
public fun longOf(a: Int, b: Int): Long {
	return a.toLong() shl 32 or (b.toLong() and 0xffffffffL)
}
