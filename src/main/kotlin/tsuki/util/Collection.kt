@file:JvmName("CollectionUtils")

package tsuki.util

import androidx.collection.ArrayMap
import androidx.collection.ArraySet
import java.util.ArrayList
import java.util.Collections

/**
 * Returns the size of this collection, or `0` if the reference is `null`.
 */
public fun Collection<*>?.sizeOrZero(): Int = this?.size ?: 0

/**
 * Clears this [MutableCollection] and populates it with all items from [subject].
 * Pre-allocates backing capacity if this is an [ArrayList].
 */
public fun <T> MutableCollection<T>.replaceWith(subject: Iterable<T>) {
	if (this is ArrayList && subject is Collection<*>) {
		ensureCapacity(size + subject.size)
	}
	clear()
	addAll(subject)
}

/**
 * Flattens each nested iterable of this sequence into the provided [destination] collection.
 *
 * @param destination The mutable collection to append flattened items into.
 * @return The populated [destination] collection.
 */
public fun <T, C : MutableCollection<in T>> Iterable<Iterable<T>>.flattenTo(destination: C): C {
	for (element in this) {
		destination.addAll(element)
	}
	return destination
}

/**
 * Returns the middle element of the list, or `null` if the list is empty.
 */
public fun <T> List<T>.medianOrNull(): T? = when {
	isEmpty() -> null
	else -> get((size / 2).coerceIn(indices))
}

/**
 * Transforms each element of this collection and returns the results in an Android [ArraySet].
 */
public inline fun <T, R> Collection<T>.mapToSet(transform: (T) -> R): Set<R> {
	return mapTo(ArraySet(size), transform)
}

/**
 * Transforms each element of this collection, discarding `null` results, into an Android [ArraySet].
 */
public inline fun <T, R : Any> Collection<T>.mapNotNullToSet(transform: (T) -> R?): Set<R> {
	val destination = ArraySet<R>(size)
	for (item in this) {
		destination.add(transform(item) ?: continue)
	}
	return destination
}

/**
 * Returns a typed array containing the results of applying [transform] to each element in this array.
 */
public inline fun <T, reified R> Array<T>.mapToArray(transform: (T) -> R): Array<R> = Array(size) { i ->
	transform(get(i))
}

/**
 * Converts a list of key-value pairs into an Android memory-efficient [ArrayMap].
 */
public fun <K, V> List<Pair<K, V>>.toMutableMap(): MutableMap<K, V> = toMap(ArrayMap(size))

/**
 * Moves an item at [sourceIndex] to [targetIndex] in-place by rotating the sublist.
 */
public fun <T> MutableList<T>.move(sourceIndex: Int, targetIndex: Int) {
	if (sourceIndex <= targetIndex) {
		Collections.rotate(subList(sourceIndex, targetIndex + 1), -1)
	} else {
		Collections.rotate(subList(targetIndex, sourceIndex + 1), 1)
	}
}

/**
 * Returns the next element from this iterator, or `null` if the iterator has no more elements.
 */
public fun <T> Iterator<T>.nextOrNull(): T? = if (hasNext()) next() else null

/**
 * Groups elements into a [Map] where each key maps to a [Set] of transformed values.
 */
public inline fun <T, K, V> Collection<T>.associateGrouping(transform: (T) -> Pair<K, V>): Map<K, Set<V>> {
	val initialCapacity = if (size < 3) size + 1 else (size / 0.75f + 1.0f).toInt()
	val result = LinkedHashMap<K, MutableSet<V>>(initialCapacity)
	for (item in this) {
		val (k, v) = transform(item)
		result.getOrPut(k) { LinkedHashSet() }.add(v)
	}
	return result
}

/**
 * Increments the integer value associated with [key] by 1 (defaulting from 0 if absent) and returns the new value.
 */
public fun <K> MutableMap<K, Int>.incrementAndGet(key: K): Int {
	var value = get(key) ?: 0
	value++
	put(key, value)
	return value
}

/**
 * Creates a new [MutableSet] of size [size] using an [ArraySet], populated via [init].
 */
public inline fun <T> MutableSet(size: Int, init: (index: Int) -> T): MutableSet<T> {
	val set = ArraySet<T>(size)
	repeat(size) { index -> set.add(init(index)) }
	return set
}

/**
 * Creates an immutable [Set] of size [size], optimizing for 0, 1, or N elements.
 */
public inline fun <T> Set(size: Int, init: (index: Int) -> T): Set<T> = when (size) {
	0 -> emptySet()
	1 -> Collections.singleton(init(0))
	else -> MutableSet(size, init)
}

/**
 * Returns an array containing the results of applying [transform] to each element in this [Collection].
 */
public inline fun <T, reified R> Collection<T>.mapToArray(transform: (T) -> R): Array<R> {
	var index = 0
	return Array(size) {
		transform(elementAt(index++))
	}
}

/**
 * Converts this array to a memory-efficient [Set] (using [ArraySet] for multiple elements).
 */
public fun <T> Array<T>.toArraySet(): Set<T> = when (size) {
	0 -> emptySet()
	1 -> setOf(first())
	else -> ArraySet(this)
}

/**
 * Converts this collection to a memory-efficient [Set] (using [ArraySet] for multiple elements).
 */
public fun <T> Collection<T>.toArraySet(): Set<T> = when (size) {
	0 -> emptySet()
	1 -> setOf(first())
	else -> ArraySet(this)
}
