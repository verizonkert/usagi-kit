package tsuki.util.json

import org.json.JSONObject

/**
 * An [Iterable] view over the key-value entries of a [JSONObject] with typed values of type [T].
 *
 * Emulates [Map.entries] without converting the entire JSON object into a separate JVM [Map].
 *
 * @param T The expected value type.
 * @param json The underlying JSON object.
 * @param typeClass The Java [Class] used for type checking and casting.
 */
public class JSONObjectTypedIterableWrapper<T : Any>(
	private val json: JSONObject,
	private val typeClass: Class<T>,
) : Iterable<Map.Entry<String, T>> {

	override fun iterator(): Iterator<Map.Entry<String, T>> = IteratorImpl()

	private inner class IteratorImpl : Iterator<Map.Entry<String, T>> {

		private val keyIterator = json.keys().iterator()

		override fun hasNext(): Boolean = keyIterator.hasNext()

		override fun next(): Map.Entry<String, T> {
			val key = keyIterator.next()
			val raw = json.opt(key)
			val value = if (raw === null || raw === JSONObject.NULL) null else raw
			return JSONEntry(key, typeClass.cast(value))
		}
	}

	private class JSONEntry<T : Any>(override val key: String, override val value: T) : Map.Entry<String, T>
}
