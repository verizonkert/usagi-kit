@file:Suppress("DEPRECATION")

package tsuki.model.search

import androidx.collection.ArrayMap
import androidx.collection.ArraySet
import tsuki.model.SortOrder

/**
 * Legacy search query container for filtering and sorting manga search results.
 *
 * This class is immutable and must be constructed using [Builder].
 *
 * @property criteria The set of search criteria applied to the query.
 * @property order The sorting order for the results, or `null` to use default order.
 * @property offset The item pagination offset index.
 * @property skipValidation Whether to bypass capability checks when processing this query.
 */
@Deprecated(
	message = "MangaSearchQuery is deprecated in favor of MangaListFilter.",
	replaceWith = ReplaceWith("MangaListFilter", "tsuki.model.MangaListFilter"),
)
@ConsistentCopyVisibility
public data class MangaSearchQuery private constructor(
	@JvmField public val criteria: Set<QueryCriteria<*>>,
	@JvmField public val order: SortOrder?,
	@JvmField public val offset: Int,
	@JvmField public val skipValidation: Boolean,
) {

	/**
	 * Creates a new [Builder] pre-populated with criteria and values from this query.
	 */
	public fun newBuilder(): Builder = Builder(this)

	/**
	 * Builder for constructing immutable [MangaSearchQuery] instances.
	 */
	public class Builder {

		private val criteria = ArraySet<QueryCriteria<*>>()
		private var order: SortOrder? = null
		private var offset: Int = 0
		private var skipValidation: Boolean = false

		/**
		 * Creates an empty builder.
		 */
		public constructor()

		/**
		 * Creates a builder initialized with values from an existing [query].
		 */
		public constructor(query: MangaSearchQuery) : this() {
			criteria.addAll(query.criteria)
			order = query.order
			offset = query.offset
		}

		/**
		 * Adds a search [criterion] to the query.
		 */
		public fun criterion(criterion: QueryCriteria<*>): Builder = apply { criteria.add(criterion) }

		/**
		 * Sets the desired sort [order].
		 */
		public fun order(order: SortOrder?): Builder = apply { this.order = order }

		/**
		 * Sets the pagination [offset].
		 */
		public fun offset(offset: Int): Builder = apply { this.offset = offset }

		/**
		 * Configures whether parser capability validation should be skipped.
		 */
		public fun skipValidation(skip: Boolean): Builder = apply { this.skipValidation = skip }

		/**
		 * Deduplicates combined criteria and builds the immutable [MangaSearchQuery].
		 *
		 * @throws IllegalArgumentException If duplicate contradictory match or range criteria exist for the same field.
		 */
		@Throws(IllegalArgumentException::class)
		public fun build(): MangaSearchQuery {
			return MangaSearchQuery(deduplicateCriteria(criteria), order, offset, skipValidation)
		}

		private fun deduplicateCriteria(criteria: Set<QueryCriteria<*>>): Set<QueryCriteria<*>> {
			val uniqueCriteria =
				ArrayMap<Pair<SearchableField, Class<out QueryCriteria<*>>>, QueryCriteria<*>>(criteria.size)

			for (criterion in criteria) {
				val key = criterion.field to criterion::class.java
				val existing = uniqueCriteria[key]

				when {
					existing == null -> uniqueCriteria[key] = criterion

					existing is QueryCriteria.Include<*> && criterion is QueryCriteria.Include<*> -> {
						uniqueCriteria[key] =
							QueryCriteria.Include(criterion.field, existing.values union criterion.values)
					}

					existing is QueryCriteria.Exclude<*> && criterion is QueryCriteria.Exclude<*> -> {
						uniqueCriteria[key] =
							QueryCriteria.Exclude(criterion.field, existing.values union criterion.values)
					}

					else -> throw IllegalArgumentException(
						"Match and Range have only one criterion per type, but found duplicates for: ${criterion.field} in ${criterion::class.simpleName}",
					)
				}
			}

			return uniqueCriteria.values.toSet()
		}
	}

	public companion object {

		public val EMPTY: MangaSearchQuery = MangaSearchQuery(emptySet(), null, 0, false)
	}
}
