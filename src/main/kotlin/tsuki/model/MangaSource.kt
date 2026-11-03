package tsuki.model

/**
 * Metadata descriptor representing a registered manga provider or website parser plugin.
 *
 * Each source defines identity attributes (unique enum/slug [name], user-visible [title],
 * primary [locale], predominant [contentType], and a health flag [isBroken]).
 */
public interface MangaSource {

	/** Unique system identifier for this manga source. */
	public val name: String

	/** User-visible display title of the source (defaults to [name]). */
	public val title: String get() = name

	/**
	 * Primary language tag of the source content using standard IETF BCP 47 language codes
	 * (e.g. `"en"`, `"ja"`, or empty string for multi-language sources).
	 */
	public val locale: String get() = ""

	/** Predominant content type hosted by this source (defaults to [ContentType.OTHER]). */
	public val contentType: ContentType get() = ContentType.OTHER

	/** Flag indicating whether the upstream website is permanently defunct, blocked, or unavailable. */
	public val isBroken: Boolean get() = false
}

