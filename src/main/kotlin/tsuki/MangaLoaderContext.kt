package tsuki

import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Response
import tsuki.bitmap.Bitmap
import tsuki.config.MangaSourceConfig
import tsuki.model.MangaSource
import tsuki.util.LinkResolver
import java.util.Base64
import java.util.Locale

/**
 * Host environment bridge providing platform resources to [MangaParser] instances.
 *
 * [MangaLoaderContext] is implemented by the host application (Android App or Desktop test runner)
 * to furnish parsers with HTTP clients, cookie management, JavaScript evaluation, image decoders,
 * and persistent source configurations without coupling the parser to specific OS APIs.
 */
public abstract class MangaLoaderContext {

	/**
	 * Pre-configured [OkHttpClient] used for executing network requests.
	 * Managed by the host application with shared connection pools, cache, and DNS resolution.
	 */
	public abstract val httpClient: OkHttpClient

	/**
	 * Shared [CookieJar] managing HTTP cookies across requests and webviews.
	 */
	public abstract val cookieJar: CookieJar

	/**
	 * Factory method creating a new [MangaParser] instance for the requested [MangaSource].
	 */
	public abstract fun newParserInstance(source: MangaSource): MangaParser

	/**
	 * Returns all manga sources supported and registered in this host context.
	 */
	public abstract fun getParserSources(): List<MangaSource>

	/**
	 * Creates a new [LinkResolver] instance for resolving a title from an [HttpUrl].
	 */
	public fun newLinkResolver(link: HttpUrl): LinkResolver = LinkResolver(link, context = this)

	/**
	 * Creates a new [LinkResolver] instance for resolving a title from a String URL.
	 */
	public fun newLinkResolver(link: String): LinkResolver = newLinkResolver(link.toHttpUrl())

	/**
	 * Standard Base64 encoder helper.
	 */
	public open fun encodeBase64(data: ByteArray): String = Base64.getEncoder().encodeToString(data)

	/**
	 * Standard Base64 decoder helper.
	 */
	public open fun decodeBase64(data: String): ByteArray = Base64.getDecoder().decode(data)

	/**
	 * Returns preferred user locales in order of priority (e.g. for multilingual sources).
	 */
	public open fun getPreferredLocales(): List<Locale> = listOf(Locale.getDefault())

	/**
	 * Executes JavaScript code and returns the result as a String.
	 *
	 * @param script JavaScript source code to evaluate.
	 * @return Evaluation result as a string, or null if execution failed or returned undefined.
	 */
	@Deprecated("Provide a base url")
	public abstract suspend fun evaluateJs(script: String): String?

	/**
	 * Executes JavaScript code within the security context of a [baseUrl] and returns the result.
	 *
	 * Useful for headless token extraction, unpacking obfuscated JS scripts, or interacting
	 * with local storage APIs.
	 *
	 * @param baseUrl Base URL of the page against which the script will be executed.
	 * @param script JavaScript source code to evaluate.
	 * @return Evaluation result as a string, or null if evaluation yielded no output.
	 */
	public abstract suspend fun evaluateJs(baseUrl: String, script: String): String?

	/**
	 * Opens [url] in an external or interactive browser (e.g. for manual captcha solving or web login).
	 *
	 * @throws UnsupportedOperationException if interactive browser actions are unavailable in the host environment.
	 */
	public open fun requestBrowserAction(parser: MangaParser, url: String): Nothing {
		throw UnsupportedOperationException("Browser is not available")
	}

	/**
	 * Returns the configuration storage for the specified [source].
	 */
	public abstract fun getConfig(source: MangaSource): MangaSourceConfig

	/**
	 * Returns the default system User-Agent string used by the host application.
	 */
	public abstract fun getDefaultUserAgent(): String

	/**
	 * Interceptor helper used to descramble/unscramble puzzle manga images.
	 *
	 * @param response Original image network response containing scrambled image data.
	 * @param redraw Transformation lambda that reads the scrambled [Bitmap] and outputs the descrambled [Bitmap].
	 * @return A new [Response] containing the descrambled image bytes.
	 */
	public abstract fun redrawImageResponse(
		response: Response,
		redraw: (image: Bitmap) -> Bitmap,
	): Response

	/**
	 * Creates a new empty [Bitmap] with the given dimensions for drawing and image composition.
	 *
	 * @param width Canvas width in pixels.
	 * @param height Canvas height in pixels.
	 * @return An initialized [Bitmap] instance ready for [Bitmap.drawBitmap] calls.
	 */
	public abstract fun createBitmap(
		width: Int,
		height: Int,
	): Bitmap
}
