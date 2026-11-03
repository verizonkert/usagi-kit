@file:JvmName("MangaParsersUtils")

package tsuki.util

import tsuki.model.MangaChapter
import tsuki.model.MangaListFilter
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * Returns `true` if this filter is either `null` or contains no active search or filter constraints.
 */
@OptIn(ExperimentalContracts::class)
public fun MangaListFilter?.isNullOrEmpty(): Boolean {
	contract {
		returns(false) implies (this@isNullOrEmpty != null)
	}
	return this == null || this.isEmpty()
}

/**
 * Searches this collection for a [MangaChapter] matching [chapterId].
 *
 * Uses direct indexed loop lookup when the collection is a [List] to prevent iterator allocations.
 *
 * @param chapterId Target chapter identifier.
 * @return The matching [MangaChapter], or `null` if not found.
 */
public fun Collection<MangaChapter>.findById(chapterId: Long): MangaChapter? {
	if (this is List<MangaChapter>) {
		for (i in 0 until size) {
			val chapter = get(i)
			if (chapter.id == chapterId) {
				return chapter
			}
		}
		return null
	}
	return find { it.id == chapterId }
}
