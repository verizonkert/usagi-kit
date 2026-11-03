package tsuki.util.json

import org.json.JSONArray

/**
 * A bi-directional, type-casting [ListIterator] over elements of a [JSONArray].
 *
 * @param T The expected target element type (e.g. [org.json.JSONObject], [String]).
 * @param array The backing JSON array.
 * @param typeClass The Java [Class] used to cast each element dynamically.
 * @param startIndex The initial cursor position within the array.
 */
public class JSONArrayTypedIterator<T : Any>(
	private val array: JSONArray,
	private val typeClass: Class<T>,
	startIndex: Int,
) : ListIterator<T> {

	private val total = array.length()
	private var index = startIndex.coerceIn(0, total)

	override fun hasNext() = index < total

	override fun next(): T {
		if (!hasNext()) throw NoSuchElementException()
		return get(index++)
	}

	override fun hasPrevious(): Boolean = index > 0

	override fun nextIndex(): Int = index

	override fun previous(): T {
		if (!hasPrevious()) throw NoSuchElementException()
		return get(--index)
	}

	override fun previousIndex(): Int = index - 1

	private fun get(i: Int): T = typeClass.cast(array[i])
}
