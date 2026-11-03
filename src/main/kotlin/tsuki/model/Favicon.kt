package tsuki.model

import okhttp3.HttpUrl.Companion.toHttpUrl

/**
 * Represents a website favicon or application icon parsed from an upstream source website.
 *
 * Implements [Comparable] to prioritize icons by size (pixels) and relationship attribute weight
 * (e.g. prioritizing `apple-touch-icon` over standard icons).
 *
 * @property url Absolute or relative URL pointing to the icon resource.
 * @property size Icon dimension (width/height in pixels, or 0 if unknown/vector).
 * @property rel HTML `<link>` relationship attribute value (e.g. `"icon"`, `"apple-touch-icon"`), or `null`.
 */
public data class Favicon(
	@JvmField public val url: String,
	@JvmField public val size: Int,
	@JvmField internal val rel: String?,
) : Comparable<Favicon> {

	/**
	 * Inferred file extension format (e.g. `"png"`, `"svg"`, `"ico"`), or empty string if undetectable.
	 */
	@JvmField
	public val type: String = url.toHttpUrl().pathSegments.last()
		.substringAfterLast('.', "").lowercase()

	override fun compareTo(other: Favicon): Int {
		val res = size.compareTo(other.size)
		if (res != 0) {
			return res
		}
		return relWeightOf(rel).compareTo(relWeightOf(other.rel))
	}

	private fun relWeightOf(rel: String?) = when (rel) {
		"apple-touch-icon" -> 1 // Prefer apple-touch-icon because it has a better quality
		"mask-icon" -> -1
		else -> 0
	}
}

