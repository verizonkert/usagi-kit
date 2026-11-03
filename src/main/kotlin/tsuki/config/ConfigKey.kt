package tsuki.config

/**
 * Strongly typed descriptor representing a configuration entry in [MangaSourceConfig].
 *
 * Each key defines a unique identifier string [key] and a fallback [defaultValue].
 * Subclasses define specialized configuration options such as mirror domains, user-agents,
 * image server choices, or custom user inputs.
 *
 * @param T The type of value associated with this configuration key.
 * @property key The unique string identifier used to persist and look up this configuration entry.
 */
public sealed class ConfigKey<T>(
	@JvmField public val key: String,
) {

	/**
	 * The default fallback value applied when no explicit value has been configured or persisted.
	 */
	public abstract val defaultValue: T

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is ConfigKey<*>) return false
		return key == other.key
	}

	override fun hashCode(): Int = key.hashCode()

	override fun toString(): String = "ConfigKey($key)"

	/**
	 * Configuration key for the source's website domain and mirror addresses.
	 *
	 * Parsers use this key to let users switch between alternative working mirrors or proxies
	 * when an ISP blocks the primary domain.
	 *
	 * @param presetValues Array of supported domains. The first entry serves as the default domain.
	 * @throws IllegalArgumentException if [presetValues] is empty.
	 */
	public class Domain(
		@JvmField @JvmSuppressWildcards public vararg val presetValues: String,
	) : ConfigKey<String>("domain") {

		init {
			require(presetValues.isNotEmpty()) { "You must provide at least one domain" }
		}

		/**
		 * Default domain, resolved to the first entry of [presetValues].
		 */
		override val defaultValue: String
			get() = presetValues.first()
	}

	/**
	 * Configuration key indicating whether content flagged as suspicious or unverified
	 * should be displayed in search/browse listings.
	 *
	 * @param defaultValue Whether suspicious content is displayed by default.
	 */
	public class ShowSuspiciousContent(
		override val defaultValue: Boolean,
	) : ConfigKey<Boolean>("show_suspicious")

	/**
	 * Configuration key storing the HTTP `User-Agent` header value dispatched with parser requests.
	 *
	 * @param defaultValue Default User-Agent string, typically supplied by the host application environment.
	 */
	public class UserAgent(
		override val defaultValue: String,
	) : ConfigKey<String>("user_agent")

	/**
	 * Configuration key specifying whether manga chapters or entries should be grouped and split
	 * by translation groups/scanlators.
	 *
	 * @param defaultValue Default splitting behavior.
	 */
	public class SplitByTranslations(
		override val defaultValue: Boolean,
	) : ConfigKey<Boolean>("split_translations")

	/**
	 * Configuration key allowing users to choose an upstream image CDN or mirror server.
	 *
	 * @param presetValues Map of user-visible labels to internal server URLs/identifiers.
	 * @param defaultValue Default image server identifier, or `null` to use the primary host.
	 */
	public class PreferredImageServer(
		public val presetValues: Map<String?, String?>,
		override val defaultValue: String?,
	) : ConfigKey<String?>("img_server")

	/**
	 * Configuration key for open-ended user-provided text input, such as custom API tokens,
	 * cookie strings, or proxy hostnames.
	 *
	 * Stored under the key prefix `"input.<key>"`.
	 *
	 * @param key The sub-key identifier for this user input option.
	 */
	public class UserInput(
		key: String,
	) : ConfigKey<String?>("input.$key") {
		override val defaultValue: String? = null
	}
}

