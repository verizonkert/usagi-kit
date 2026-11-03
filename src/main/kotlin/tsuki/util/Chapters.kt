package tsuki.util

import org.json.JSONArray
import org.json.JSONObject
import tsuki.InternalParsersApi
import tsuki.model.MangaChapter
import tsuki.util.json.asTypedList

/**
 * Transforms elements of this [List] into [MangaChapter] instances, deduplicating them by [MangaChapter.id].
 *
 * @param reversed If `true`, iterates over the elements in reverse order without allocating an extra copy.
 * @param transform Lambda producing a [MangaChapter] or `null` for each element along with its sequential index.
 * @return A deduplicated list of non-null [MangaChapter] instances.
 */
@InternalParsersApi
public inline fun <T> List<T>.mapChapters(
	reversed: Boolean = false,
	transform: (index: Int, T) -> MangaChapter?,
): List<MangaChapter> {
	val builder = ChaptersListBuilder(collectionSize())
	var index = 0
	val elements = if (reversed) this.asReversed() else this
	for (item in elements) {
		if (builder.add(transform(index, item))) {
			index++
		}
	}
	return builder.toList()
}

/**
 * Transforms elements of this [Iterable] into [MangaChapter] instances, deduplicating them by [MangaChapter.id].
 *
 * @param reversed If `true`, iterates over the elements in reverse order.
 * @param transform Lambda producing a [MangaChapter] or `null` for each element along with its sequential index.
 * @return A deduplicated list of non-null [MangaChapter] instances.
 */
@InternalParsersApi
public inline fun <T> Iterable<T>.mapChapters(
	reversed: Boolean = false,
	transform: (index: Int, T) -> MangaChapter?,
): List<MangaChapter> {
	return if (this is List<T>) {
		this.mapChapters(reversed, transform)
	} else {
		val list = if (reversed) this.reversed() else this.toList()
		list.mapChapters(reversed = false, transform)
	}
}

/**
 * Transforms a [JSONArray] of [JSONObject] elements into [MangaChapter] instances, deduplicating them by [MangaChapter.id].
 *
 * @param reversed If `true`, iterates over the JSON array in reverse order.
 * @param transform Lambda producing a [MangaChapter] or `null` for each JSON object along with its sequential index.
 * @return A deduplicated list of non-null [MangaChapter] instances.
 */
@InternalParsersApi
public inline fun JSONArray.mapChapters(
	reversed: Boolean = false,
	transform: (index: Int, JSONObject) -> MangaChapter?,
): List<MangaChapter> = asTypedList<JSONObject>().mapChapters(reversed, transform)

/**
 * Maps each element of this [List] to an [Iterable] of chapters and flattens the result into a deduplicated list.
 *
 * @param reversed If `true`, processes the list in reverse order without allocating an extra copy.
 * @param transform Lambda producing an iterable of [MangaChapter] items for each element.
 * @return A deduplicated list of non-null [MangaChapter] instances.
 */
@InternalParsersApi
public inline fun <T> List<T>.flatMapChapters(
	reversed: Boolean = false,
	transform: (T) -> Iterable<MangaChapter?>,
): List<MangaChapter> {
	val builder = ChaptersListBuilder(collectionSize())
	val elements = if (reversed) this.asReversed() else this
	for (item in elements) {
		builder.addAll(transform(item))
	}
	return builder.toList()
}

/**
 * Maps each element of this [Iterable] to an [Iterable] of chapters and flattens the result into a deduplicated list.
 *
 * @param reversed If `true`, processes the elements in reverse order.
 * @param transform Lambda producing an iterable of [MangaChapter] items for each element.
 * @return A deduplicated list of non-null [MangaChapter] instances.
 */
@InternalParsersApi
public inline fun <T> Iterable<T>.flatMapChapters(
	reversed: Boolean = false,
	transform: (T) -> Iterable<MangaChapter?>,
): List<MangaChapter> {
	return if (this is List<T>) {
		this.flatMapChapters(reversed, transform)
	} else {
		val list = if (reversed) this.reversed() else this.toList()
		list.flatMapChapters(reversed = false, transform)
	}
}

/**
 * Returns the estimated or exact size of this [Iterable], defaulting to 10 if unknown.
 */
public fun <T> Iterable<T>.collectionSize(): Int {
	return if (this is Collection<*>) this.size else 10
}

/**
 * Efficient builder for collecting [MangaChapter] instances while maintaining insertion order
 * and ensuring chapter IDs remain unique.
 *
 * @param initialSize Estimated number of chapters to pre-allocate capacity for.
 */
public class ChaptersListBuilder(initialSize: Int = 10) {

	private val capacity = if (initialSize > 0) (initialSize * 4 / 3) + 1 else 16
	private val ids = HashSet<Long>(capacity)
	private val list = ArrayList<MangaChapter>(initialSize.coerceAtLeast(0))

	/**
	 * Adds a [chapter] if it is non-null and its [MangaChapter.id] has not been added yet.
	 *
	 * @return `true` if the chapter was added; `false` if it was null or a duplicate ID.
	 */
	public fun add(chapter: MangaChapter?): Boolean {
		return chapter != null && ids.add(chapter.id) && list.add(chapter)
	}

	/**
	 * Adds all non-null, uniquely identified chapters from [chapters].
	 */
	public fun addAll(chapters: Iterable<MangaChapter?>) {
		if (chapters is Collection<*>) {
			list.ensureCapacity(list.size + chapters.size)
		}
		chapters.forEach { add(it) }
	}

	/**
	 * Operator overload for adding a single [chapter].
	 *
	 * @see add
	 */
	public operator fun plusAssign(chapter: MangaChapter?) {
		add(chapter)
	}

	/**
	 * Reverses the current order of collected chapters in place.
	 */
	public fun reverse() {
		list.reverse()
	}

	/**
	 * Returns the accumulated list of unique [MangaChapter] instances.
	 */
	public fun toList(): List<MangaChapter> = list
}
