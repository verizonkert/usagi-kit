@file:Suppress("DEPRECATION")

package tsuki

import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import tsuki.config.ConfigKey
import tsuki.config.MangaSourceConfig
import tsuki.model.Favicons
import tsuki.model.Manga
import tsuki.model.MangaChapter
import tsuki.model.MangaListFilter
import tsuki.model.MangaListFilterCapabilities
import tsuki.model.MangaListFilterOptions
import tsuki.model.MangaPage
import tsuki.model.MangaSource
import tsuki.model.SortOrder
import tsuki.model.search.MangaSearchQuery
import tsuki.model.search.MangaSearchQueryCapabilities
import java.util.EnumSet

/**
 * Core interface for all manga and comic source parsers.
 *
 * An implementation of [MangaParser] represents a specific manga website or API integration.
 * The host reader application interacts with parsers through a standardized lifecycle:
 *
 * 1. **Discovery & Exploration**:
 *    - [getList]: Fetches paginated titles based on search queries, genre filters, and sort orders.
 *    - [getFilterOptions]: Returns dynamic or static filter dropdowns, tags, and demographics.
 * 2. **Detail Extraction**:
 *    - [getDetails]: Hydrates title metadata (description, author, alt titles, and complete chapter lists).
 * 3. **Reading & Media**:
 *    - [getPages]: Extracts the list of pages ([MangaPage]) for a specific [MangaChapter].
 *    - [getPageUrl]: Resolves direct image CDN links (or tokenized URLs) for reading.
 * 4. **Network & Interception**:
 *    - Inherits from [Interceptor], allowing parsers to inject custom headers (e.g. `Referer`, `User-Agent`)
 *      and perform automated rate limiting or Cloudflare handling on outgoing HTTP requests.
 */
public interface MangaParser : Interceptor {

	/**
	 * Unique identifier and metadata descriptor of this source.
	 * Corresponds to the annotation generated via [MangaSourceParser].
	 */
	public val source: MangaSource

	/**
	 * Supported [SortOrder] variants (e.g. POPULARITY, UPDATED, RATING, RELEVANCE).
	 * Must contain at least one item.
	 *
	 * For optimal runtime performance and memory efficiency, prefer using [EnumSet].
	 */
	public val availableSortOrders: Set<SortOrder>

	@Deprecated(
		message = "Replaced by filterCapabilities, which provides a simplified and unified filter model.",
		replaceWith = ReplaceWith("filterCapabilities"),
	)
	public val searchQueryCapabilities: MangaSearchQueryCapabilities

	/**
	 * Declares search and filter capabilities supported by this parser (such as genre inclusion/exclusion,
	 * search-with-filters, publication year ranges, etc.).
	 */
	public val filterCapabilities: MangaListFilterCapabilities

	/**
	 * Configuration store providing access to user-selected preferences, such as custom domains
	 * or image server preferences.
	 */
	public val config: MangaSourceConfig

	/**
	 * Optional provider for sources that require user login or authentication.
	 * Automatically casts `this` if the parser implements [MangaParserAuthProvider].
	 */
	public val authorizationProvider: MangaParserAuthProvider?
		get() = this as? MangaParserAuthProvider

	/**
	 * Config key defining the default domain and all available alternative mirror domains.
	 *
	 * **Best Practice**: Never hardcode domains in network requests. Always build URLs using [domain].
	 */
	public val configKeyDomain: ConfigKey.Domain

	/**
	 * Current active domain name (e.g. `"manga.org"`), resolving user mirror selection from [config].
	 */
	public val domain: String

	@Deprecated(
		message = "Replaced by getList(offset, order, filter) which uses the unified MangaListFilter.",
		replaceWith = ReplaceWith("getList(offset = 0, order = SortOrder.POPULARITY, filter = filter)"),
	)
	public suspend fun getList(query: MangaSearchQuery): List<Manga>

	/**
	 * Fetches a paginated list of manga matching the specified filter criteria and sort order.
	 *
	 * @param offset Starting index for pagination (usually `0, pageSize, 2 * pageSize...`).
	 * @param order Desired sorting strategy from [availableSortOrders].
	 * @param filter Search filters including title keywords, genres, demographics, and publication states.
	 * @return List of matching [Manga] instances.
	 */
	public suspend fun getList(offset: Int, order: SortOrder, filter: MangaListFilter): List<Manga>

	/**
	 * Parses comprehensive details for the specified [manga], including chapter list, full description,
	 * authors, tags, and high-resolution cover image.
	 *
	 * **Contract**: Must return a copy of the given [manga]. Implementations may mutate or populate
	 * any field except [Manga.id], [Manga.url], and [Manga.source], which are immutable anchors.
	 *
	 * @see Manga.copy
	 */
	public suspend fun getDetails(manga: Manga): Manga

	/**
	 * Parses the complete list of pages for the given [chapter].
	 *
	 * @param chapter The chapter whose pages should be loaded.
	 * @return List of [MangaPage] items with relative or absolute image URLs.
	 */
	public suspend fun getPages(chapter: MangaChapter): List<MangaPage>

	/**
	 * Resolves the direct, fetchable image URL for a given [page].
	 *
	 * For sources where [getPages] already provides direct image URLs, this typically returns [MangaPage.url].
	 * For sources with signed tokens or delayed image loaders, this method resolves the final link.
	 */
	public suspend fun getPageUrl(page: MangaPage): String

	/**
	 * Fetches all selectable filter options (genres, categories, content ratings) available for this source.
	 */
	public suspend fun getFilterOptions(): MangaListFilterOptions

	/**
	 * Discovers and extracts site favicons or high-res touch icons from the source website.
	 */
	public suspend fun getFavicons(): Favicons

	/**
	 * Hook invoked when initializing configuration preferences for this parser.
	 * Parsers may register additional custom [ConfigKey] instances into [keys].
	 */
	public fun onCreateConfig(keys: MutableCollection<ConfigKey<*>>)

	/**
	 * Discovers recommendations or related manga titles associated with the given [seed] title.
	 */
	public suspend fun getRelatedManga(seed: Manga): List<Manga>

	/**
	 * Returns HTTP headers required for network operations against this source (e.g. `Referer`, `User-Agent`).
	 */
	public fun getRequestHeaders(): Headers

	/**
	 * Resolves a [Manga] object from an external web URL (e.g. from deep links or clipboard).
	 *
	 * @param link Web URL pointing to a title on this source's website.
	 * @return Resolved [Manga] instance, or null if the URL does not belong to this source.
	 */
	public suspend fun resolveLink(link: HttpUrl): Manga? {
		return resolveLink(tsuki.util.LinkResolver(link, parser = this), link)
	}

	@Deprecated(
		message = "Replaced by resolveLink(link). The separate LinkResolver parameter is no longer required.",
		replaceWith = ReplaceWith("resolveLink(link)"),
	)
	public suspend fun resolveLink(resolver: tsuki.util.LinkResolver, link: HttpUrl): Manga? = null

	/**
	 * Backward-compatible overload for resolving links from String URLs.
	 */
	public suspend fun resolveLink(link: String): Manga? {
		val url = link.toHttpUrlOrNull() ?: return null
		return resolveLink(url)
	}
}
