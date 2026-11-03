package tsuki.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import tsuki.MangaLoaderContext
import tsuki.MangaParser
import tsuki.model.Manga
import tsuki.model.MangaListFilter
import tsuki.model.MangaSource
import tsuki.model.RATING_UNKNOWN
import tsuki.model.SortOrder

/**
 * Resolves full [Manga] entities from external web links (e.g. from browser deep links or clipboard sharing).
 *
 * Employs direct parser link resolution first, falling back to seed creation and search verification.
 *
 * @property link The external HTTP URL to resolve.
 * @property context Optional loader context for global resolution.
 * @property parser Optional target parser responsible for parsing this URL's domain.
 */
public class LinkResolver(
	public val link: HttpUrl,
	public val context: MangaLoaderContext? = null,
	public val parser: MangaParser? = null,
) {

	/**
	 * Returns the source metadata associated with this resolver's target parser, if bound.
	 */
	public suspend fun getSource(): MangaSource? = parser?.source

	/**
	 * Resolves and returns the full [Manga] representation corresponding to [link].
	 *
	 * @return The resolved [Manga] instance, or `null` if the link cannot be resolved.
	 */
	public suspend fun getManga(): Manga? {
		val p = parser ?: return null // TODO: implement global link resolution via context
		return p.resolveLink(link) ?: resolveManga(p)
	}

	/**
	 * Constructs a synthetic stub manga for [url] and enhances it using [parser].
	 *
	 * @param parser The parser to resolve details with.
	 * @param url Relative URL path of the manga.
	 * @param id Unique internal identifier generated for this source and URL.
	 * @param title Placeholder title while details are being resolved.
	 */
	public suspend fun resolveManga(
		parser: MangaParser,
		url: String = link.toString().toRelativeUrl(link.host),
		id: Long = generateUid(parser.source, url),
		title: String = STUB_TITLE,
	): Manga? = resolveBySeed(
		parser,
		Manga(
			id = id,
			title = title,
			altTitles = emptySet(),
			url = url,
			publicUrl = link.toString(),
			rating = RATING_UNKNOWN,
			contentRating = null,
			coverUrl = "",
			tags = emptySet(),
			state = null,
			authors = emptySet(),
			largeCoverUrl = null,
			description = null,
			chapters = null,
			source = parser.source,
		),
	)

	/**
	 * Resolves full manga information from a seed object, verifying against source search if supported.
	 */
	private suspend fun resolveBySeed(parser: MangaParser, s: Manga): Manga? {
		val seed = parser.getDetails(s)
		if (!parser.filterCapabilities.isSearchSupported) {
			return seed.takeUnless { it.chapters.isNullOrEmpty() }
		}
		val query = when {
			seed.title != STUB_TITLE && seed.title.isNotEmpty() -> seed.title
			seed.altTitles.isNotEmpty() -> seed.altTitles.first()
			seed.authors.isNotEmpty() -> seed.authors.first()
			else -> return seed // unfortunately we do not know a real manga title so unable to find it
		}
		val resolved = runCatching {
			val list = parser.getList(0, parser.bestSortOrder(), MangaListFilter(query = query))
			list.singleOrNull { manga -> isSameUrl(manga.publicUrl) }
		}.getOrNull()
		if (resolved == null) {
			return seed
		}
		return runCatching {
			parser.getDetails(resolved)
		}.getOrElse {
			resolved.copy(
				chapters = seed.chapters ?: resolved.chapters,
				description = seed.description ?: resolved.description,
				authors = seed.authors.ifEmpty { resolved.authors },
				tags = seed.tags + resolved.tags,
				state = seed.state ?: resolved.state,
				coverUrl = seed.coverUrl ?: resolved.coverUrl,
				largeCoverUrl = seed.largeCoverUrl ?: resolved.largeCoverUrl,
				altTitles = seed.altTitles + resolved.altTitles,
			)
		}
	}

	/**
	 * Checks if [publicUrl] points to the same target web destination as [link].
	 */
	private fun isSameUrl(publicUrl: String): Boolean {
		if (publicUrl == link.toString()) {
			return true
		}
		val httpUrl = publicUrl.toHttpUrlOrNull() ?: return false
		return link.host == httpUrl.host
			&& link.encodedPath.trimEnd('/') == httpUrl.encodedPath.trimEnd('/')
	}

	/**
	 * Selects the optimal [SortOrder] for search verification (preferring [SortOrder.RELEVANCE]).
	 */
	private fun MangaParser.bestSortOrder(): SortOrder {
		val supported = availableSortOrders
		if (SortOrder.RELEVANCE in supported) {
			return SortOrder.RELEVANCE
		}
		return SortOrder.entries.first { it in supported }
	}

	private companion object {

		/**
		 * Default placeholder title assigned to synthetic seed [Manga] instances
		 * before their actual title is parsed from the source details page or API.
		 */
		const val STUB_TITLE = "Unknown manga"
	}
}
