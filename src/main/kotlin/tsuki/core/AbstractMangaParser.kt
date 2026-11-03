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
import tsuki.util.convertToMangaListFilter
import tsuki.util.toAbsoluteUrl
import tsuki.util.toMangaSearchQueryCapabilities
import java.util.Locale

/**
 * Base abstract implementation of [MangaParser] providing unified configuration,
 * client initialization, URL resolution, and backward-compatible search adapters.
 */
@InternalParsersApi
public abstract class AbstractMangaParser @InternalParsersApi constructor(
	@property:InternalParsersApi public val context: MangaLoaderContext,
	public final override val source: MangaSource,
) : MangaParser {

	/**
	 * Legacy search query capability bridge for backward compatibility.
	 */
	@Suppress("OVERRIDE_DEPRECATION")
	public final override val searchQueryCapabilities: MangaSearchQueryCapabilities
		get() = filterCapabilities.toMangaSearchQueryCapabilities()

	/**
	 * Lazy-loaded source configuration store.
	 */
	public override val config: MangaSourceConfig by lazy { context.getConfig(source) }

	/**
	 * The resolved [Locale] of the source, cached and safely built from standard language tags.
	 */
	public open val sourceLocale: Locale by lazy {
		val tag = source.locale.trim()
		if (tag.isEmpty()) Locale.ROOT else Locale.forLanguageTag(tag)
	}

	/**
	 * Returns [ContentRating.ADULT] if this is a dedicated adult/hentai source, or null otherwise.
	 */
	protected val sourceContentRating: ContentRating?
		get() = if (isNsfwSource) ContentRating.ADULT else null

	/**
	 * Convenience flag indicating whether this source is categorized as adult/NSFW.
	 */
	protected val isNsfwSource: Boolean = source.contentType == ContentType.HENTAI

	/**
	 * Configuration key for the User-Agent string.
	 */
	protected open val userAgentKey: ConfigKey.UserAgent = ConfigKey.UserAgent(context.getDefaultUserAgent())

	/**
	 * Base HTTP headers dispatched with requests made by this parser.
	 */
	override fun getRequestHeaders(): Headers = Headers.Builder()
		.add(CommonHeaders.USER_AGENT, config[userAgentKey])
		.build()

	/**
	 * Safe fallback [SortOrder] when an incoming request does not specify one.
	 */
	public open val defaultSortOrder: SortOrder
		get() {
			val supported = availableSortOrders
			return supported.firstOrNull()
				?: SortOrder.entries.firstOrNull { it in supported }
				?: SortOrder.POPULARITY
		}

	/**
	 * The primary domain configured for this source.
	 */
	final override val domain: String
		get() = config[configKeyDomain]

	/**
	 * The HTTP client assigned to this parser.
	 */
	protected open val webClient: WebClient = OkHttpWebClient(context.httpClient, source)

	/**
	 * Legacy adapter delegating deprecated [MangaSearchQuery] to the modern filter pipeline.
	 */
	@Suppress("OVERRIDE_DEPRECATION")
	public final override suspend fun getList(query: MangaSearchQuery): List<Manga> = getList(
		offset = query.offset,
		order = query.order ?: defaultSortOrder,
		filter = convertToMangaListFilter(query),
	)

	/**
	 * Resolves relative page image URLs against the configured domain.
	 */
	public override suspend fun getPageUrl(page: MangaPage): String = page.url.toAbsoluteUrl(domain)

	/**
	 * Extracts favicon entries from the root website of the source.
	 */
	public override suspend fun getFavicons(): Favicons {
		return FaviconParser(webClient, domain).parseFavicons()
	}

	@CallSuper
	public override fun onCreateConfig(keys: MutableCollection<ConfigKey<*>>) {
		keys.add(configKeyDomain)
	}

	/**
	 * Discovers similar or related manga using title similarity.
	 * Skips search queries if the source does not support searching.
	 */
	public override suspend fun getRelatedManga(seed: Manga): List<Manga> {
		if (!filterCapabilities.isSearchSupported) return emptyList()
		return RelatedMangaFinder(listOf(this)).invoke(seed)
	}

	/**
	 * Resolves deep links to manga detail items. Returns null by default.
	 */
	override suspend fun resolveLink(resolver: LinkResolver, link: HttpUrl): Manga? = null

	/**
	 * Interceptor hook for modifying or decorating requests/responses on the fly.
	 */
	override fun intercept(chain: Interceptor.Chain): Response = chain.proceed(chain.request())
}
