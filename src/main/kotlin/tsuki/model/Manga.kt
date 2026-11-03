package tsuki.model

import androidx.collection.ArrayMap
import tsuki.util.findById
import tsuki.util.nullIfEmpty

/**
 * Core domain model representing a manga, manhwa, manhua, novel, or comic entity.
 *
 * Encapsulates core metadata, author information, content ratings, cover links,
 * tags/genres, publication status, and optional chapter lists.
 *
 * @property id Unique numeric identifier assigned to this manga.
 * @property title Primary human-readable title of the manga.
 * @property altTitles Set of alternative titles (e.g. localized names, romanizations), or empty.
 * @property url Relative URL or path identifier (without domain), used by parsers.
 * @property publicUrl Absolute public webpage URL ready to open in an external browser.
 * @property rating Normalized rating in the range `0.0..1.0`, or [RATING_UNKNOWN] if unrated.
 * @property contentRating Content maturity rating ([ContentRating.SAFE], [ContentRating.SUGGESTIVE], or [ContentRating.ADULT]), or `null`.
 * @property coverUrl Absolute URL pointing to the primary cover image thumbnail.
 * @property tags Set of genre/theme tags associated with this manga.
 * @property state Publication state ([MangaState.ONGOING], [MangaState.FINISHED], etc.), or `null`.
 * @property authors Set of author and artist names.
 * @property largeCoverUrl Absolute URL pointing to a high-resolution cover image, or `null`.
 * @property description Full textual summary or synopsis (may contain HTML markup), or `null`.
 * @property chapters Optional list of parsed [MangaChapter] instances, or `null` if not yet loaded.
 * @property source The [MangaSource] from which this manga was parsed.
 */
public data class Manga(
	@JvmField public val id: Long,
	@JvmField public val title: String,
	@JvmField public val altTitles: Set<String>,
	@JvmField public val url: String,
	@JvmField public val publicUrl: String,
	@JvmField public val rating: Float,
	@JvmField public val contentRating: ContentRating?,
	@JvmField public val coverUrl: String?,
	@JvmField public val tags: Set<MangaTag>,
	@JvmField public val state: MangaState?,
	@JvmField public val authors: Set<String>,
	@JvmField public val largeCoverUrl: String? = null,
	@JvmField public val description: String? = null,
	@JvmField public val chapters: List<MangaChapter>? = null,
	@JvmField public val source: MangaSource,
) {

	/**
	 * Legacy constructor accepting single author and single altTitle strings.
	 */
	@Deprecated("Use primary constructor with Set<String> authors and altTitles instead")
	public constructor(
		id: Long,
		title: String,
		altTitle: String?,
		url: String,
		publicUrl: String,
		rating: Float,
		isNsfw: Boolean,
		coverUrl: String?,
		tags: Set<MangaTag>,
		state: MangaState?,
		author: String?,
		largeCoverUrl: String? = null,
		description: String? = null,
		chapters: List<MangaChapter>? = null,
		source: MangaSource,
	) : this(
		id = id,
		title = title,
		altTitles = setOfNotNull(altTitle?.nullIfEmpty()),
		url = url,
		publicUrl = publicUrl,
		rating = rating,
		contentRating = if (isNsfw) ContentRating.ADULT else null,
		coverUrl = coverUrl?.nullIfEmpty(),
		tags = tags,
		state = state,
		authors = setOfNotNull(author),
		largeCoverUrl = largeCoverUrl?.nullIfEmpty(),
		description = description?.nullIfEmpty(),
		chapters = chapters,
		source = source,
	)

	/**
	 * Primary author of the manga, resolved to the first entry of [authors].
	 */
	@Deprecated("Please use authors", ReplaceWith("authors.firstOrNull()"))
	public val author: String?
		get() = authors.firstOrNull()

	/**
	 * Primary alternative title of the manga, resolved to the first entry of [altTitles].
	 */
	@Deprecated("Please use altTitles", ReplaceWith("altTitles.firstOrNull()"))
	public val altTitle: String?
		get() = altTitles.firstOrNull()

	/**
	 * Returns `true` if this manga has a valid positive score between `0.0` and `1.0`.
	 */
	public val hasRating: Boolean
		get() = rating > 0f && rating <= 1f

	/**
	 * Indicates whether this manga is marked as adult/NSFW content.
	 */
	@Deprecated("Use contentRating instead", ReplaceWith("contentRating == ContentRating.ADULT"))
	public val isNsfw: Boolean
		get() = contentRating == ContentRating.ADULT

	/**
	 * Returns all chapters that belong to the specified release [branch], or all chapters if [branch] is `null`.
	 */
	public fun getChapters(branch: String?): List<MangaChapter> {
		return chapters?.filter { x -> x.branch == branch }.orEmpty()
	}

	/**
	 * Finds a chapter by its unique [id], or returns `null` if not found.
	 */
	public fun findChapterById(id: Long): MangaChapter? = chapters?.findById(id)

	/**
	 * Finds a chapter by its unique [id], or throws [NoSuchElementException] if not found.
	 */
	public fun requireChapterById(id: Long): MangaChapter = findChapterById(id)
		?: throw NoSuchElementException("Chapter with id $id not found")

	/**
	 * Computes a frequency map of release branch names to their respective chapter count.
	 */
	public fun getBranches(): Map<String?, Int> {
		if (chapters.isNullOrEmpty()) {
			return emptyMap()
		}
		val result = ArrayMap<String?, Int>()
		chapters.forEach {
			val key = it.branch
			result[key] = result.getOrDefault(key, 0) + 1
		}
		return result
	}
}

