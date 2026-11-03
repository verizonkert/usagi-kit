@file:Suppress("DEPRECATION")

package tsuki.model.search

import androidx.collection.ArraySet
import tsuki.model.search.QueryCriteria.Exclude
import tsuki.model.search.QueryCriteria.Include
import tsuki.model.search.QueryCriteria.Match
import tsuki.model.search.QueryCriteria.Range
import tsuki.util.mapToSet

/**
 * Defines the collective search capabilities of a manga source in the legacy query pipeline.
 *
 * Encapsulates the set of supported [SearchCapability] rules and validates incoming [MangaSearchQuery]
 * instances against exclusive field constraints and multi-value rules via [validate].
 *
 * @property capabilities The set of search field capabilities supported by the source.
 */
@Deprecated(
	message = "MangaSearchQueryCapabilities is deprecated in favor of MangaListFilterCapabilities.",
	replaceWith = ReplaceWith("MangaListFilterCapabilities", "tsuki.model.MangaListFilterCapabilities"),
)
@ExposedCopyVisibility
public data class MangaSearchQueryCapabilities internal constructor(
	public val capabilities: Set<SearchCapability>,
) {

	/**
	 * Convenience constructor accepting variable arguments of [SearchCapability].
	 */
	public constructor(vararg capabilities: SearchCapability) : this(ArraySet(capabilities))

	/**
	 * Validates the given [query] against the source's declared capabilities.
	 *
	 * @param query The [MangaSearchQuery] to inspect and validate.
	 * @throws IllegalArgumentException If an unsupported field or criterion is present,
	 * or if multi-value / exclusive field restrictions are violated.
	 */
	public fun validate(query: MangaSearchQuery) {
		val strictFields = capabilities.filter { it.isExclusive }.mapToSet { it.field }
		val usedStrictFields = query.criteria.mapToSet { it.field }.intersect(strictFields)

		require(usedStrictFields.isEmpty() || query.criteria.size <= 1) {
			"Query contains multiple criteria, but at least one field (${usedStrictFields.joinToString()}) does not support multiple criteria."
		}
		for (criterion in query.criteria) {
			val capability = requireNotNull(capabilities.find { it.field == criterion.field }) {
				"Unsupported search field: ${criterion.field}"
			}

			require(criterion::class in capability.criteriaTypes) {
				"Unsupported search criterion: ${criterion::class.simpleName} for field ${criterion.field}"
			}

			// Ensure single value per criterion if supportMultiValue is false
			if (!capability.isMultiple) {
				when (criterion) {
					is Include<*> -> require(criterion.values.size <= 1) {
						"Multiple values are not allowed for field ${criterion.field}"
					}

					is Exclude<*> -> require(criterion.values.size <= 1) {
						"Multiple values are not allowed for field ${criterion.field}"
					}

					is Range<*> -> Unit // Range is always valid (from, to)
					is Match<*> -> Unit // Match always has a single value
				}
			}
		}
	}
}
