package tsuki.model

/**
 * Immutable sorted collection of [Favicon] items parsed from a website.
 *
 * Automatically sorts contained icons in descending priority order (largest/highest quality first)
 * and retains an optional HTTP [referer] header required to download them.
 *
 * @param favicons The collection of parsed favicons.
 * @property referer Optional HTTP `Referer` required to bypass hotlink protection when fetching the icon images.
 */
public class Favicons(
	favicons: Collection<Favicon>,
	@JvmField public val referer: String?,
) : Collection<Favicon> {

	private val icons = favicons.sortedDescending()

	override val size: Int
		get() = icons.size

	override fun contains(element: Favicon): Boolean = icons.contains(element)

	override fun containsAll(elements: Collection<Favicon>): Boolean = icons.containsAll(elements)

	override fun isEmpty(): Boolean = icons.isEmpty()

	override fun iterator(): Iterator<Favicon> = icons.iterator()

	/**
	 * Returns a new [Favicons] collection omitting [victim].
	 */
	public operator fun minus(victim: Favicon): Favicons = Favicons(
		favicons = icons.filterNot { it == victim },
		referer = referer,
	)

	/**
	 * Finds the smallest favicon whose size in pixels is greater than or equal to [size].
	 *
	 * If no icon meets the minimum size requirement, returns the largest available icon.
	 *
	 * @param size Minimum required icon size in pixels.
	 * @param types Optional set of allowed file extensions (e.g. `"png"`, `"svg"`), or `null` for any type.
	 * @return The best matching [Favicon], or `null` if the collection is empty.
	 */
	@JvmOverloads
	public fun find(size: Int, types: Set<String>? = null): Favicon? {
		if (icons.isEmpty()) {
			return null
		}
		var result: Favicon? = null
		for (icon in icons) {
			if (types != null && icon.type !in types) {
				continue
			}
			if (result == null || icon.size >= size) {
				result = icon
			} else {
				break
			}
		}
		return result
	}

	public companion object {

		/** Empty [Favicons] collection singleton. */
		@JvmStatic
		public val EMPTY: Favicons = Favicons(emptySet(), null)

		/** Creates a [Favicons] instance containing a single icon at [url]. */
		@JvmStatic
		public fun single(url: String): Favicons = Favicons(setOf(Favicon(url, 0, null)), null)
	}
}

