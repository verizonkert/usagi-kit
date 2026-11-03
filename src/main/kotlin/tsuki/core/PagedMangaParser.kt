package tsuki.core

import androidx.annotation.VisibleForTesting
import tsuki.InternalParsersApi
import tsuki.MangaLoaderContext
import tsuki.model.Manga
import tsuki.model.MangaListFilter
import tsuki.model.MangaSource
import tsuki.model.SortOrder
import tsuki.util.Paginator

/**
 * Primary abstract base class for paginated manga parsers.
 *
 * Automatically translates the application's continuous [offset] pagination into upstream website
 * page indices, dynamically adapting to the website's real-world page sizes.
 *
 * Maintains two separate [Paginator] instances:
 * - [paginator]: For standard catalog browsing and updates.
 * - [searchPaginator]: For search queries and filtered listings.
 *
 * @param context Environment context supplying network clients and configuration.
 * @param source Metadata descriptor for this manga source.
 * @param pageSize Expected number of items per page during standard catalog browsing.
 * @param searchPageSize Expected number of items per page during search queries (defaults to [pageSize]).
 */
@InternalParsersApi
public abstract class PagedMangaParser @InternalParsersApi constructor(
	context: MangaLoaderContext,
	source: MangaSource,
	@VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
	@JvmField
	public val pageSize: Int,
	searchPageSize: Int = pageSize,
) : AbstractMangaParser(context, source) {

	/**
	 * Paginator managing standard catalog / latest updates offsets.
	 */
	@JvmField
	protected val paginator: Paginator = Paginator(pageSize)

	/**
	 * Paginator managing search and filtered query offsets.
	 */
	@JvmField
	protected val searchPaginator: Paginator = Paginator(searchPageSize)

	/**
	 * Determines whether the incoming [filter] represents a search operation
	 * requiring the use of [searchPaginator] instead of [paginator].
	 *
	 * Subclasses can override this to customize search routing.
	 */
	protected open fun isSearchFilter(filter: MangaListFilter): Boolean {
		return !filter.query.isNullOrEmpty()
	}

	/**
	 * Translates continuous [offset] into a 1-based (or 0-based) page number, fetches items via
	 * [getListPage], and calibrates the paginator with the actual item count received.
	 */
	final override suspend fun getList(offset: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
		val activePaginator = if (isSearchFilter(filter)) searchPaginator else paginator

		val page = activePaginator.getPage(offset)
		val list = getListPage(page, order, filter)
		activePaginator.onListReceived(offset, page, list.size)
		return list
	}

	/**
	 * Fetches a single page of manga from the upstream website.
	 *
	 * @param page The calculated page index (1-based by default, or 0-based if configured with [setFirstPage]).
	 * @param order The sorting order requested for the list.
	 * @param filter The active filter criteria (query, tags, publication states, etc.).
	 * @return List of parsed [Manga] entities for this page.
	 */
	public abstract suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga>

	/**
	 * Configures the initial page index for catalog browsing and search.
	 *
	 * Most websites start pagination at page `1` (the default), but some start at page `0`.
	 *
	 * @param firstPage Starting page index for general catalog browsing.
	 * @param firstPageForSearch Starting page index for search queries (defaults to [firstPage]).
	 */
	protected fun setFirstPage(firstPage: Int, firstPageForSearch: Int = firstPage) {
		paginator.firstPage = firstPage
		searchPaginator.firstPage = firstPageForSearch
	}
}
