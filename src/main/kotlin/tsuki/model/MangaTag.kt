package tsuki.model

/**
 * Represents a genre, topic, theme, author, or category tag associated with a manga source.
 *
 * Tags are scoped to a specific [source] to allow independent taxonomies per catalog.
 *
 * @property title User-readable localized tag label (preferred in Title Case, e.g. "Action", "Romance").
 * @property key Unique source-specific identifier/slug used for querying and network requests (e.g. `"action"` or `"12"`).
 * @property source The [MangaSource] to which this tag belongs.
 */
public data class MangaTag(
	@JvmField public val title: String,
	@JvmField public val key: String,
	@JvmField public val source: MangaSource,
)

