@file:Suppress("DEPRECATION")

package tsuki.core

import androidx.annotation.CallSuper
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import tsuki.InternalParsersApi
import tsuki.MangaLoaderContext
import tsuki.MangaParser
import tsuki.config.ConfigKey
import tsuki.config.MangaSourceConfig
import tsuki.model.ContentRating
import tsuki.model.ContentType
import tsuki.model.Favicons
import tsuki.model.Manga
import tsuki.model.MangaListFilter
import tsuki.model.MangaListFilterCapabilities
import tsuki.model.MangaPage
import tsuki.model.MangaSource
import tsuki.model.SortOrder
import tsuki.model.search.MangaSearchQuery
import tsuki.model.search.MangaSearchQueryCapabilities
import tsuki.network.CommonHeaders
import tsuki.network.OkHttpWebClient
import tsuki.network.WebClient
import tsuki.util.FaviconParser
import tsuki.util.LinkResolver
import tsuki.util.RelatedMangaFinder
import tsuki.util.convertToMangaSearchQuery
import tsuki.util.toAbsoluteUrl
import tsuki.util.toMangaListFilterCapabilities
import java.util.Locale

/**
 * Legacy base class for manga parsers designed around [MangaSearchQuery].
 *
 * @deprecated Use [AbstractMangaParser] which operates directly on [MangaListFilter].
 * Retained for backwards binary compatibility with existing plugins.
 */
@Deprecated(
	message = "FlexibleMangaParser is deprecated in favor of AbstractMangaParser, which operates directly on MangaListFilter without unnecessary complexity.",
	replaceWith = ReplaceWith("AbstractMangaParser(context, source)", "tsuki.core.AbstractMangaParser"),
)
public abstract class FlexibleMangaParser @InternalParsersApi constructor(
	@property:InternalParsersApi public val context: MangaLoaderContext,
	public final override val source: MangaSource,
) : MangaParser {

	/**
	 * Lazy-loaded configuration store for this source.
	 */
	override val config: MangaSourceConfig by lazy { context.getConfig(source) }

	/**
	 * Resolved and cached [Locale] using standard IETF BCP 47 language tags.
	 */
	public open val sourceLocale: Locale by lazy {
		val tag = source.locale.trim()
		if (tag.isEmpty()) Locale.ROOT else Locale.forLanguageTag(tag)
	}

	/**
	 * Configuration key for the User-Agent header.
	 */
	protected open val userAgentKey: ConfigKey.UserAgent = ConfigKey.UserAgent(context.getDefaultUserAgent())

	/**
	 * Translates legacy [searchQueryCapabilities] into modern [MangaListFilterCapabilities].
	 */
	public final override val filterCapabilities: MangaListFilterCapabilities
		get() = searchQueryCapabilities.toMangaListFilterCapabilities()

	/**
	 * Returns [ContentRating.ADULT] for hentai sources, or null otherwise.
	 */
	protected val sourceContentRating: ContentRating?
		get() = if (source.contentType == ContentType.HENTAI) ContentRating.ADULT else null

	/**
	 * Primary domain configured for this source.
	 */
	public final override val domain: String
		get() = config[configKeyDomain]

	/**
	 * Default request headers including configured User-Agent.
	 */
	@Deprecated("Override intercept() instead")
	override fun getRequestHeaders(): Headers = Headers.Builder()
		.add(CommonHeaders.USER_AGENT, config[userAgentKey])
		.build()

	/**
	 * Safe fallback [SortOrder] preventing NoSuchElementException.
	 */
	public open val defaultSortOrder: SortOrder
		get() {
			val supported = availableSortOrders
			return supported.firstOrNull()
				?: SortOrder.entries.firstOrNull { it in supported }
				?: SortOrder.POPULARITY
		}

	/**
	 * HTTP client allocated for this parser.
	 */
	protected open val webClient: WebClient = OkHttpWebClient(context.httpClient, source)

	/**
	 * Resolves page image relative URLs to absolute URLs.
	 */
	public override suspend fun getPageUrl(page: MangaPage): String = page.url.toAbsoluteUrl(domain)

	/**
	 * Bridge adapting modern [MangaListFilter] requests into the legacy [MangaSearchQuery] pipeline.
	 */
	public final override suspend fun getList(offset: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
		return getList(convertToMangaSearchQuery(offset, order, filter))
	}

	/**
	 * Fetches favicons from the main website domain.
	 */
	public override suspend fun getFavicons(): Favicons {
		return FaviconParser(webClient, domain).parseFavicons()
	}

	@CallSuper
	public override fun onCreateConfig(keys: MutableCollection<ConfigKey<*>>) {
		keys.add(configKeyDomain)
	}

	/**
	 * Finds related manga by title similarity, guarded against sources that do not support searching.
	 */
	public override suspend fun getRelatedManga(seed: Manga): List<Manga> {
		if (!filterCapabilities.isSearchSupported) return emptyList()
		return RelatedMangaFinder(listOf(this)).invoke(seed)
	}

	/**
	 * Deep-link resolver for web links.
	 */
	@Deprecated("Use resolveLink(HttpUrl) instead")
	override suspend fun resolveLink(resolver: LinkResolver, link: HttpUrl): Manga? = null

	/**
	 * OkHttp interceptor hook for request decoration.
	 */
	override fun intercept(chain: Interceptor.Chain): Response = chain.proceed(chain.request())
}
