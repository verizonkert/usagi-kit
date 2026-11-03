package tsuki.util

import org.jsoup.nodes.Element
import tsuki.util.attrOrNull
import tsuki.util.nullIfEmpty
import tsuki.util.splitByWhitespace

/**
 * Utility data structure for parsing inline CSS `background`, `background-image`, and `background-position` properties.
 *
 * Frequently used to decode manga sources that stitch or sprite thumbnail images into a single combined sheet
 * and render individual covers via CSS background offsets and dimensions.
 *
 * @property url Direct image URL extracted from `url(...)`.
 * @property left Horizontal pixel offset from the left edge (positive value).
 * @property top Vertical pixel offset from the top edge (positive value).
 * @property width Width in pixels of the visible viewport element.
 * @property height Height in pixels of the visible viewport element.
 */
public class CSSBackground private constructor(
	public val url: String,
	public val left: Int,
	public val top: Int,
	public val width: Int,
	public val height: Int,
) {

	/**
	 * Calculated right boundary pixel offset (`left + width`).
	 */
	public val right: Int
		get() = left + width

	/**
	 * Calculated bottom boundary pixel offset (`top + height`).
	 */
	public val bottom: Int
		get() = top + height

	internal companion object {

		/**
		 * Parses a [CSSBackground] from the inline `style` attribute of a Jsoup [element].
		 *
		 * @param element HTML element with an inline `style` attribute containing background declarations.
		 * @return Parsed [CSSBackground] instance, or `null` if attributes or geometry cannot be determined.
		 */
		fun parse(element: Element): CSSBackground? {
			val style = element.attrOrNull("style") ?: return null
			val attrs = HashMap<String, String>()
			for (part in style.split(';')) {
				val colonIndex = part.indexOf(':')
				if (colonIndex != -1) {
					val key = part.substring(0, colonIndex).trim()
					val value = part.substring(colonIndex + 1).trim()
					attrs[key] = value
				}
			}

			val width = attrs["width"]?.toPx() ?: return null
			val height = attrs["height"]?.toPx() ?: return null

			val bgRaw = attrs["background-image"] ?: attrs["background"] ?: return null
			val urlStart = bgRaw.indexOf("url(")
			if (urlStart == -1) return null

			val urlEnd = bgRaw.indexOf(')', urlStart)
			if (urlEnd == -1) return null

			val rawUrl = bgRaw.substring(urlStart + 4, urlEnd).trim().trim('\'', '"')
			val url = rawUrl.nullIfEmpty() ?: return null

			// Check background-position or remaining background tokens
			var posX = 0
			var posY = 0
			val positionStr = attrs["background-position"]
			if (positionStr != null) {
				val tokens = positionStr.splitByWhitespace()
				posX = tokens.firstOrNull()?.toPx() ?: 0
				posY = tokens.getOrNull(1)?.toPx() ?: 0
			} else {
				val afterUrl = bgRaw.substring(urlEnd + 1).splitByWhitespace()
				posX = afterUrl.firstOrNull()?.toPx() ?: 0
				posY = afterUrl.getOrNull(1)?.toPx() ?: 0
			}

			return CSSBackground(
				url = url,
				left = if (posX < 0) -posX else posX,
				top = if (posY < 0) -posY else posY,
				width = width,
				height = height,
			)
		}

		/**
		 * Extracts the integer pixel count from a CSS measurement string (e.g. `"150px"` -> `150`).
		 */
		private fun String.toPx(): Int? {
			val clean = trim().removeSuffix("px")
			return clean.toIntOrNull()
		}
	}
}
