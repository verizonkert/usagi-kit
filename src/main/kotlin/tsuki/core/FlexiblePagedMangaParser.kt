@file:Suppress("DEPRECATION")

package tsuki.core

import androidx.annotation.VisibleForTesting
import tsuki.InternalParsersApi
import tsuki.MangaLoaderContext
import tsuki.model.Manga
import tsuki.model.MangaSource
import tsuki.model.search.MangaSearchQuery
import tsuki.model.search.SearchableField
import tsuki.util.Paginator

/**
 * Legacy paginated base parser that translates [offset] requests into 1-based or 0-based page numbers.
 *
 * Maintains two separate [Paginator] instances: one for catalog browsing and one for search queries.
 *
 * @deprecated Use [PagedMangaParser] which operates on the modern filter architecture.
 */
@Deprecated(
	message = "FlexiblePagedMangaParser is deprecated in favor of PagedMangaParser, which integrates with the unified filter pipeline.",
	replaceWith = ReplaceWith("PagedMangaParser(context, source, pageSize, searchPageSize)", "tsuki.core.PagedMangaParser"),
)
public abstract class FlexiblePagedMangaParser @InternalParsersApi constructor(
	context: MangaLoaderContext,
	source: MangaSource,
	@VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
	@JvmField
	public val pageSize: Int,
	searchPageSize: Int = pageSize,
) : FlexibleMangaParser(context, source) {

	/**
	 * Paginator managing standard catalog / latest updates offsets.
	 */
	@JvmField
	protected val paginator: Paginator = Paginator(pageSize)

	/**
	 * Paginator managing search query offsets.
	 */
	@JvmField
	protected val searchPaginator: Paginator = Paginator(searchPageSize)

	/**
	 * Translates a legacy search query into the appropriate page number and fetches the manga list.
	 */
	@Deprecated(
		message = "Replaced by getList(offset, order, filter) which uses the unified MangaListFilter.",
		replaceWith = ReplaceWith("getList(query.offset, query.order ?: defaultSortOrder, convertToMangaListFilter(query))"),
	)
	final override suspend fun getList(query: MangaSearchQuery): List<Manga> {
		val isTitleSearch = query.criteria.any { it.field == SearchableField.TITLE_NAME }
		val activePaginator = if (isTitleSearch) searchPaginator else paginator

		return fetchMangaPage(activePaginator, query)
	}

	/**
	 * Subclasses implement this method to fetch a single page of results from the upstream website.
	 *
	 * @param query The search query and filters requested.
	 * @param page The 1-based (or 0-based, depending on [setFirstPage]) page index.
	 * @return List of parsed manga for the specified page.
	 */
	public abstract suspend fun getListPage(query: MangaSearchQuery, page: Int): List<Manga>

	/**
	 * Configures the initial page index (e.g. 0 or 1) for catalog browsing and search.
	 *
	 * @param firstPage Starting page index for general browsing (defaults to 1 in [Paginator]).
	 * @param firstPageForSearch Starting page index for search queries.
	 */
	protected fun setFirstPage(firstPage: Int, firstPageForSearch: Int = firstPage) {
		paginator.firstPage = firstPage
		searchPaginator.firstPage = firstPageForSearch
	}

	/**
	 * Internal execution loop translating offset to page, invoking [getListPage],
	 * and recording actual page size feedback.
	 */
	private suspend fun fetchMangaPage(
		activePaginator: Paginator,
		query: MangaSearchQuery,
	): List<Manga> {
		val offset = query.offset
		val page = activePaginator.getPage(offset)
		val list = getListPage(query, page)
		activePaginator.onListReceived(offset, page, list.size)
		return list
	}
}
