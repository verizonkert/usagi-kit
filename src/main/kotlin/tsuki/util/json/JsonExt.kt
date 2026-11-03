package tsuki.util.json

import androidx.collection.ArraySet
import java.util.Locale
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import tsuki.util.nullIfEmpty
import kotlin.contracts.contract

/**
 * Parses this string as a [JSONObject], returning `null` if parsing fails.
 */
public fun String.toJSONObjectOrNull(): JSONObject? = try {
	JSONObject(this)
} catch (_: JSONException) {
	null
}

/**
 * Parses this string as a [JSONArray], returning `null` if parsing fails.
 */
public fun String.toJSONArrayOrNull(): JSONArray? = try {
	JSONArray(this)
} catch (_: JSONException) {
	null
}

/**
 * Maps each [JSONObject] element in this array to [R] and appends it to [destination].
 */
public inline fun <R, C : MutableCollection<in R>> JSONArray.mapJSONTo(
	destination: C,
	block: (JSONObject) -> R,
): C {
	val len = length()
	for (i in 0 until len) {
		val jo = getJSONObject(i)
		destination.add(block(jo))
	}
	return destination
}

/**
 * Maps each [JSONObject] element in this array to [R], discarding `null` items, and appends to [destination].
 */
public inline fun <R, C : MutableCollection<in R>> JSONArray.mapJSONNotNullTo(
	destination: C,
	block: (JSONObject) -> R?,
): C {
	val len = length()
	for (i in 0 until len) {
		val jo = getJSONObject(i)
		destination.add(block(jo) ?: continue)
	}
	return destination
}

/**
 * Transforms each [JSONObject] element in this [JSONArray] into a [List] of type [T].
 */
public inline fun <T> JSONArray.mapJSON(block: (JSONObject) -> T): List<T> {
	return mapJSONTo(ArrayList(length()), block)
}

/**
 * Transforms each [JSONObject] element in this [JSONArray] into a [List] of non-null [T] items.
 */
public inline fun <T : Any> JSONArray.mapJSONNotNull(block: (JSONObject) -> T?): List<T> {
	return mapJSONNotNullTo(ArrayList(length()), block)
}

/**
 * Transforms each [JSONObject] element in this [JSONArray] into an Android [ArraySet] of type [T].
 */
public inline fun <T> JSONArray.mapJSONToSet(mapper: (JSONObject) -> T): Set<T> {
	return mapJSONTo(ArraySet<T>(length()), mapper)
}

/**
 * Transforms each [JSONObject] element in this [JSONArray] into an Android [ArraySet] of non-null [T] items.
 */
public inline fun <T : Any> JSONArray.mapJSONNotNullToSet(mapper: (JSONObject) -> T?): Set<T> {
	return mapJSONNotNullTo(ArraySet<T>(length()), mapper)
}

/**
 * Transforms each [JSONObject] element in this array along with its sequential array index into a [List] of [T].
 */
public fun <T> JSONArray.mapJSONIndexed(block: (Int, JSONObject) -> T): List<T> {
	val len = length()
	val result = ArrayList<T>(len)
	for (i in 0 until len) {
		val jo = getJSONObject(i)
		result.add(block(i, jo))
	}
	return result
}

/**
 * Retrieves the string value for [name], or `null` if absent, blank, or equals [JSONObject.NULL].
 */
public fun JSONObject.getStringOrNull(name: String): String? = opt(name)?.takeUnless {
	it === JSONObject.NULL
}?.toString()?.nullIfEmpty()

/**
 * Retrieves the boolean value for [name] with loose type coercion (parsing strings/numbers), or returns [defaultValue].
 */
public fun JSONObject.getBooleanOrDefault(name: String, defaultValue: Boolean): Boolean {
	return when (val rawValue = opt(name)) {
		null, JSONObject.NULL -> defaultValue
		is Boolean -> rawValue
		is Number -> rawValue.toInt() != 0
		is String -> rawValue.lowercase(Locale.ROOT).toBooleanStrictOrNull() ?: defaultValue
		else -> defaultValue
	}
}

/**
 * Retrieves the numeric [Long] value for [name] with string coercion, or returns [defaultValue].
 */
public fun JSONObject.getLongOrDefault(name: String, defaultValue: Long): Long {
	return when (val rawValue = opt(name)) {
		null, JSONObject.NULL -> defaultValue
		is Long -> rawValue
		is Number -> rawValue.toLong()
		is String -> rawValue.toLongOrNull() ?: defaultValue
		else -> defaultValue
	}
}

/**
 * Retrieves the numeric [Int] value for [name] with string coercion, or returns [defaultValue].
 */
public fun JSONObject.getIntOrDefault(name: String, defaultValue: Int): Int {
	return when (val rawValue = opt(name)) {
		null, JSONObject.NULL -> defaultValue
		is Int -> rawValue
		is Number -> rawValue.toInt()
		is String -> rawValue.toIntOrNull() ?: defaultValue
		else -> defaultValue
	}
}

/**
 * Retrieves the numeric [Double] value for [name] with string coercion, or returns [defaultValue].
 */
public fun JSONObject.getDoubleOrDefault(name: String, defaultValue: Double): Double {
	return when (val rawValue = opt(name)) {
		null, JSONObject.NULL -> defaultValue
		is Double -> rawValue
		is Number -> rawValue.toDouble()
		is String -> rawValue.toDoubleOrNull() ?: defaultValue
		else -> defaultValue
	}
}

/**
 * Retrieves the numeric [Float] value for [name] with string coercion, or returns [defaultValue].
 */
public fun JSONObject.getFloatOrDefault(name: String, defaultValue: Float): Float {
	return when (val rawValue = opt(name)) {
		null, JSONObject.NULL -> defaultValue
		is Float -> rawValue
		is Number -> rawValue.toFloat()
		is String -> rawValue.toFloatOrNull() ?: defaultValue
		else -> defaultValue
	}
}

private val enumConstantsCache = java.util.concurrent.ConcurrentHashMap<Class<*>, Array<*>>()

/**
 * Looks up an enum constant from [name] using case-insensitive matching against [enumClass] constants.
 */
public fun <E : Enum<E>> JSONObject.getEnumValueOrNull(name: String, enumClass: Class<E>): E? {
	val enumName = getStringOrNull(name) ?: return null
	@Suppress("UNCHECKED_CAST")
	val constants = enumConstantsCache.computeIfAbsent(enumClass) {
		it.enumConstants ?: emptyArray<Any>()
	} as Array<E>
	for (i in 0 until constants.size) {
		val c = constants[i]
		if (enumName.equals(c.name, ignoreCase = true)) {
			return c
		}
	}
	return null
}

/**
 * Looks up an enum constant from [name] or returns [defaultValue] if absent or unrecognized.
 */
public fun <E : Enum<E>> JSONObject.getEnumValueOrDefault(name: String, defaultValue: E): E {
	return getEnumValueOrNull(name, defaultValue.javaClass) ?: defaultValue
}

/**
 * Returns `true` if this JSON array is either `null` or has a length of `0`.
 */
public fun JSONArray?.isNullOrEmpty(): Boolean {
	contract {
		returns(false) implies (this@isNullOrEmpty != null)
	}

	return this == null || this.length() == 0
}

/**
 * Wraps this [JSONArray] as a read-only [List] casting each element to [typeClass] on access.
 */
public fun <T : Any> JSONArray.asTypedList(typeClass: Class<T>): List<T> {
	return JSONArrayTypedListWrapper(this, typeClass)
}

/**
 * Wraps this [JSONArray] as a read-only [List] casting each element to [T] on access.
 */
public inline fun <reified T : Any> JSONArray.asTypedList(): List<T> = asTypedList(T::class.java)

/**
 * Provides an [Iterable] over the entries of this [JSONObject] with values typed as [typeClass].
 */
public fun <T : Any> JSONObject.entries(typeClass: Class<T>): Iterable<Map.Entry<String, T>> {
	return JSONObjectTypedIterableWrapper(this, typeClass)
}

/**
 * Provides an [Iterable] over the entries of this [JSONObject] with values typed as [T].
 */
public inline fun <reified T : Any> JSONObject.entries(): Iterable<Map.Entry<String, T>> = entries(T::class.java)

/**
 * Converts this [JSONArray] of strings into an Android [ArraySet] of non-null, non-empty strings.
 */
public fun JSONArray.toStringSet(): Set<String> {
	val len = length()
	val set = ArraySet<String>(len)
	for (i in 0 until len) {
		val str = optString(i)
		if (!str.isNullOrEmpty()) {
			set.add(str)
		}
	}
	return set
}
