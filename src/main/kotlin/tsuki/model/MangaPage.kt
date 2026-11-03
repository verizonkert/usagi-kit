package tsuki.model

/**
 * Represents a single page image or resource within a [MangaChapter].
 *
 * @property id Unique identifier for this page within the chapter or database.
 * @property url Relative URL or path to the page image (without domain), or an internal URI resolved via the parser.
 * @property preview Absolute URL of a low-resolution thumbnail image if available, or `null`.
 * @property source The [MangaSource] from which this page originates.
 */
public data class MangaPage(
	@JvmField public val id: Long,
	@JvmField public val url: String,
	@JvmField public val preview: String?,
	@JvmField public val source: MangaSource,
)

