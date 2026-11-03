package tsuki.model

import tsuki.util.formatSimple
import tsuki.util.ifNullOrEmpty

/**
 * Represents a discrete chapter, episode, or issue belonging to a [Manga] entry.
 *
 * @property id Unique identifier for this chapter.
 * @property title Optional human-readable subtitle or title of the chapter (excluding manga title or chapter number).
 * @property number Chapter sequence number starting from 1.0, or 0.0 if unknown or unnumbered.
 * @property volume Volume number starting from 1, or 0 if unknown.
 * @property url Relative URL or path to the chapter (without domain), resolved by the parser.
 * @property scanlator Display name of the translation group, scanner, or uploader, or `null`.
 * @property uploadDate Chapter publication or upload timestamp in epoch milliseconds.
 * @property branch Translation group, language dialect, or release branch identifier, or `null`.
 * @property source The [MangaSource] from which this chapter originated.
 */
public data class MangaChapter(
	@JvmField public val id: Long,
	@JvmField public val title: String?,
	@JvmField public val number: Float,
	@JvmField public val volume: Int,
	@JvmField public val url: String,
	@JvmField public val scanlator: String?,
	@JvmField public val uploadDate: Long,
	@JvmField public val branch: String?,
	@JvmField public val source: MangaSource,
) {

	/**
	 * Formatted display name for the chapter, incorporating volume and chapter numbers if [title] is missing.
	 */
	@Deprecated("Use title instead", ReplaceWith("title"))
	val name: String
		get() = title.ifNullOrEmpty {
			buildString {
				if (volume > 0) append("Vol ").append(volume).append(' ')
				if (number > 0) append("Chapter ").append(number.formatSimple()) else append("Unnamed")
			}
		}

	/**
	 * Formats [number] as a user-friendly string (e.g. `"12"` or `"12.5"`), or returns `null` if [number] <= 0.
	 */
	public fun numberString(): String? = if (number > 0f) {
		number.formatSimple()
	} else {
		null
	}

	/**
	 * Formats [volume] as a string, or returns `null` if [volume] <= 0.
	 */
	public fun volumeString(): String? = if (volume > 0) {
		volume.toString()
	} else {
		null
	}
}

