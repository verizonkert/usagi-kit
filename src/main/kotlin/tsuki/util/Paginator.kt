package tsuki.util

import androidx.collection.SparseArrayCompat
import androidx.collection.set

public class Paginator(public val initialPageSize: Int) {
	private val lock = Any()
	public var firstPage: Int = 1
	private val pages = SparseArrayCompat<Int>()

	public fun getPage(offset: Int): Int {
		if (offset <= 0) {
			return firstPage
		}
		synchronized(lock) {
			pages[offset]?.let { return it }
		}
		val pageSize = initialPageSize.coerceAtLeast(1)
		val intPage = offset / pageSize
		val tail = offset % pageSize
		return intPage + firstPage + if (tail == 0) 0 else 1
	}

	public fun onListReceived(offset: Int, page: Int, count: Int) {
		synchronized(lock) {
			pages[offset + count] = if (count > 0) page + 1 else page
		}
	}
}
