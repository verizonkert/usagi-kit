package tsuki.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.nodes.Element
import tsuki.model.Favicon
import tsuki.model.Favicons
import tsuki.network.WebClient
import tsuki.util.json.mapJSON

/**
 * Scrapes and discovers website icons and favicons for a given domain.
 *
 * Inspects web manifest files (`rel="manifest"`), link icons (`rel="icon"`), and Apple Touch icons
 * (`rel="apple-touch-icon"`), falling back to standard `/favicon.ico` if none are discovered.
 *
 * @param webClient Network client used to fetch web pages and web app manifests.
 * @param domain Target host or URL to extract icons from.
 */
public class FaviconParser(
	private val webClient: WebClient,
	domain: String,
) {
	private val domain: String = domain.removePrefix("https://").removePrefix("http://").substringBefore('/')

	/**
	 * Parses and resolves all discoverable favicons for this domain across HTML head tags and web manifests.
	 *
	 * Executes on [Dispatchers.IO].
	 *
	 * @return A [Favicons] collection containing resolved [Favicon] items and the canonical source URL.
	 */
	public suspend fun parseFavicons(): Favicons = withContext(Dispatchers.IO) {
		val url = "https://$domain"
		val doc = webClient.httpGet(url).parseHtml()
		val result = HashSet<Favicon>()
		val manifestLink = doc.getElementsByAttributeValue("rel", "manifest").firstOrNull()
			?.attrAsAbsoluteUrlOrNull("href")
		if (manifestLink != null) {
			runCatchingCancellable {
				parseManifest(manifestLink)
			}.onSuccess { manifest ->
				result += manifest
			}
		}
		val links = doc.getElementsByAttributeValueContaining("rel", "icon")
		links.mapNotNullTo(result) { link ->
			parseLink(link)
		}
		val touchIcons = doc.getElementsByAttributeValue("rel", "apple-touch-icon")
		touchIcons.mapNotNullTo(result) { link ->
			parseLink(link)
		}
		if (result.isEmpty()) {
			result.add(createFallback())
		}
		Favicons(result, url)
	}

	/**
	 * Parses a single `<link>` element into a [Favicon] model with absolute URL and dimensions.
	 */
	private fun parseLink(link: Element): Favicon? {
		val href = link.attrAsAbsoluteUrlOrNull("href")
		if (href == null || href.endsWith('/')) {
			return null
		}
		val sizes = link.attr("sizes")
		return Favicon(
			url = href,
			size = parseSize(sizes),
			rel = link.attrOrNull("rel"),
		)
	}

	/**
	 * Parses a standard CSS/HTML `sizes` attribute (e.g. `"192x192"`, `"32x32"`, `"any"`) into an integer dimension.
	 */
	private fun parseSize(sizes: String): Int {
		if (sizes.isEmpty() || sizes == "any") {
			return 0
		}
		return sizes.substringBefore(' ')
			.split('x', 'X', '*')
			.firstNotNullOfOrNull { it.toIntOrNull() }
			?: 0
	}

	/**
	 * Downloads and parses the Web App Manifest JSON specified by [url] to extract icon definitions.
	 */
	private suspend fun parseManifest(url: String): List<Favicon> {
		val json = webClient.httpGet(url).parseJson()
		val icons = json.optJSONArray("icons") ?: return emptyList()
		return icons.mapJSON { jo ->
			Favicon(
				url = jo.getString("src").resolveLink(),
				size = parseSize(jo.getString("sizes")),
				rel = null,
			)
		}
	}

	/**
	 * Produces a default fallback [Favicon] pointing to standard `/favicon.ico`.
	 */
	private fun createFallback(): Favicon {
		val href = "https://$domain/favicon.ico"
		return Favicon(
			url = href,
			size = 0,
			rel = null,
		)
	}

	/**
	 * Resolves relative paths or protocol-relative URLs against the current domain.
	 */
	private fun String.resolveLink(): String = when {
		startsWith("http:") || startsWith("https:") -> this
		startsWith("//") -> "https:$this"
		startsWith('/') -> "https://$domain$this"
		else -> "https://$domain/$this"
	}
}
