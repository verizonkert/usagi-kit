@file:Suppress("DEPRECATION")

package tsuki.model.search

/**
 * Represents a search criterion used for filtering manga search results in legacy [MangaSearchQuery].
 *
 * Each criterion applies a specific matching, range, inclusion, or exclusion condition
 * to a [SearchableField] and operates on values of type [T].
 *
 * @param T The type of value associated with this search criterion.
 * @property field The [SearchableField] to which this search criterion applies.
 */
@Deprecated(
	message = "QueryCriteria is deprecated in favor of MangaListFilter.",
	replaceWith = ReplaceWith("MangaListFilter", "tsuki.model.MangaListFilter"),
)
public sealed interface QueryCriteria<T> {

	public val field: SearchableField

	override fun equals(other: Any?): Boolean

	override fun hashCode(): Int

	/**
	 * Represents an inclusion criterion that filters search results to items matching any of the allowed [values].
	 *
	 * @param T The type of value being included in the search.
	 * @property field The [SearchableField] to which this criterion applies.
	 * @property values The set of values that must be matched in the search results.
	 *
	 * ### Example:
	 * ```kotlin
	 * val stateFilter = QueryCriteria.Include(
	 *     field = SearchableField.STATE,
	 *     values = setOf(MangaState.ONGOING, MangaState.FINISHED),
	 * )
	 * ```
	 */
	public data class Include<T : Any>(
		public override val field: SearchableField,
		@JvmField public val values: Set<T>,
	) : QueryCriteria<T> {

		init {
			check(values.all { x -> field.type.isInstance(x) })
		}
	}

	/**
	 * Represents an exclusion criterion that filters out search results containing any of the specified [values].
	 *
	 * @param T The type of value being excluded from the search.
	 * @property field The [SearchableField] to which this criterion applies.
	 * @property values The set of values that must be excluded from the search results.
	 *
	 * ### Example:
	 * ```kotlin
	 * val excludeTag = QueryCriteria.Exclude(
	 *     field = SearchableField.TAG,
	 *     values = setOf(MangaTag(key = "horror", title = "Horror", source = source)),
	 * )
	 * ```
	 */
	public data class Exclude<T : Any>(
		public override val field: SearchableField,
		@JvmField public val values: Set<T>,
	) : QueryCriteria<T> {

		init {
			check(values.all { x -> field.type.isInstance(x) })
		}
	}

	/**
	 * Represents a range criterion that filters search results within a bounded interval [[from], [to]].
	 *
	 * @param T The comparable type of value defining the range boundaries.
	 * @property field The [SearchableField] to which this criterion applies.
	 * @property from The starting value of the range (inclusive).
	 * @property to The ending value of the range (inclusive).
	 *
	 * ### Example:
	 * ```kotlin
	 * val yearRange = QueryCriteria.Range(
	 *     field = SearchableField.PUBLICATION_YEAR,
	 *     from = 2000,
	 *     to = 2024,
	 * )
	 * ```
	 */
	public data class Range<T : Comparable<T>>(
		public override val field: SearchableField,
		@JvmField public val from: T,
		@JvmField public val to: T,
	) : QueryCriteria<T> {

		init {
			check(field.type.isInstance(from))
			check(field.type.isInstance(to))
		}
	}

	/**
	 * Represents an exact match criterion that filters search results matching the specific [value].
	 *
	 * @param T The type of value being matched.
	 * @property field The [SearchableField] to which this criterion applies.
	 * @property value The exact target value that must be matched.
	 *
	 * ### Example:
	 * ```kotlin
	 * val titleMatch = QueryCriteria.Match(
	 *     field = SearchableField.TITLE_NAME,
	 *     value = "One Piece",
	 * )
	 * ```
	 */
	public data class Match<T : Any>(
		public override val field: SearchableField,
		@JvmField public val value: T,
	) : QueryCriteria<T> {

		init {
			check(field.type.isInstance(value))
		}
	}
}
