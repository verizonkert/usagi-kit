package tsuki.core

import tsuki.InternalParsersApi
import tsuki.MangaLoaderContext
import tsuki.model.Manga
import tsuki.model.MangaListFilter
import tsuki.model.MangaSource
import tsuki.model.SortOrder

/**
 * Abstract base parser designed for manga sources that do not support pagination.
 *
 * For unpaginated sources, the entire catalog or query result is fetched on the first request ([offset] == 0).
 * Any subsequent requests ([offset] > 0) immediately return an [emptyList] to indicate to the reader
 * application that no additional items are available.
 *
 * @param context Environment context supplying network clients and configuration.
 * @param source Metadata descriptor for this manga source.
 */
@InternalParsersApi
public abstract class SinglePageMangaParser @InternalParsersApi constructor(
	context: MangaLoaderContext,
	source: MangaSource,
) : AbstractMangaParser(context, source) {

	/**
	 * Intercepts continuous [offset] pagination requests.
	 *
	 * Returns the full result on [offset] == 0, and [emptyList] for any [offset] > 0.
	 */
	final override suspend fun getList(offset: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
		if (offset > 0) {
			return emptyList()
		}
		return getList(order, filter)
	}

	/**
	 * Fetches the entire unpaginated collection of manga matching the specified [order] and [filter].
	 *
	 * @param order The sorting order requested for the list.
	 * @param filter The active filter criteria.
	 * @return List of parsed [Manga] entities.
	 */
	public abstract suspend fun getList(order: SortOrder, filter: MangaListFilter): List<Manga>
}
