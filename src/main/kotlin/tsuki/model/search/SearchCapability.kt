@file:Suppress("DEPRECATION")

package tsuki.model.search

import kotlin.reflect.KClass

/**
 * Declares parser search and filter capabilities for a specific [field].
 *
 * Defines which criterion types are accepted (such as inclusion, exclusion, or numerical ranges),
 * whether multiple values can be queried simultaneously, and whether querying this field
 * requires exclusive execution.
 *
 * @property field The [SearchableField] that this capability definition applies to.
 * @property criteriaTypes The set of supported [QueryCriteria] classes for this field (e.g. `setOf(Include::class, Exclude::class)`).
 * @property isMultiple Indicates whether multiple values can be provided for this field (e.g. multiple genre tags).
 * @property isExclusive Indicates whether this field cannot be combined with other search fields in the same query.
 */
@Deprecated(
	message = "SearchCapability is deprecated in favor of MangaListFilterCapabilities.",
	replaceWith = ReplaceWith("MangaListFilterCapabilities", "tsuki.model.MangaListFilterCapabilities"),
)
public data class SearchCapability(
	/** The searchable field that this capability applies to. */
	@JvmField public val field: SearchableField,
	/** The set of supported criteria types for this field. */
	@JvmField public val criteriaTypes: Set<KClass<out QueryCriteria<*>>>,
	/** Indicates whether the field supports multiple values. */
	@JvmField public val isMultiple: Boolean,
	/** Specifies whether the field can be used alongside other criteria. */
	@JvmField public val isExclusive: Boolean = false,
)

