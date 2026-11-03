@file:Suppress("DEPRECATION")

package tsuki.util

import tsuki.model.MangaListFilter
import tsuki.model.MangaListFilterCapabilities
import tsuki.model.SortOrder
import tsuki.model.YEAR_UNKNOWN
import tsuki.model.search.MangaSearchQuery
import tsuki.model.search.MangaSearchQueryCapabilities
import tsuki.model.search.QueryCriteria
import tsuki.model.search.QueryCriteria.Exclude
import tsuki.model.search.QueryCriteria.Include
import tsuki.model.search.QueryCriteria.Match
import tsuki.model.search.QueryCriteria.Range
import tsuki.model.search.SearchCapability
import tsuki.model.search.SearchableField.AUTHOR
import tsuki.model.search.SearchableField.CONTENT_RATING
import tsuki.model.search.SearchableField.CONTENT_TYPE
import tsuki.model.search.SearchableField.DEMOGRAPHIC
import tsuki.model.search.SearchableField.LANGUAGE
import tsuki.model.search.SearchableField.ORIGINAL_LANGUAGE
import tsuki.model.search.SearchableField.PUBLICATION_YEAR
import tsuki.model.search.SearchableField.STATE
import tsuki.model.search.SearchableField.TAG
import tsuki.model.search.SearchableField.TITLE_NAME

/**
 * Converts a modern [MangaListFilter] into a legacy [MangaSearchQuery].
 *
 * This function translates filter attributes in [MangaListFilter] into corresponding
 * search criteria in a [MangaSearchQuery.Builder] for backward compatibility with older parsers.
 *
 * @param offset Starting pagination index.
 * @param sortOrder Desired sort ordering strategy.
 * @param filter The [MangaListFilter] to convert.
 * @return A [MangaSearchQuery] constructed based on the given [filter].
 */
public fun convertToMangaSearchQuery(offset: Int, sortOrder: SortOrder, filter: MangaListFilter): MangaSearchQuery {
	return MangaSearchQuery.Builder().apply {
		offset(offset)
		order(sortOrder)
		if (filter.tags.isNotEmpty()) criterion(Include(TAG, filter.tags))
		if (filter.tagsExclude.isNotEmpty()) criterion(Exclude(TAG, filter.tagsExclude))
		if (filter.states.isNotEmpty()) criterion(Include(STATE, filter.states))
		if (filter.types.isNotEmpty()) criterion(Include(CONTENT_TYPE, filter.types))
		if (filter.contentRating.isNotEmpty()) criterion(Include(CONTENT_RATING, filter.contentRating))
		if (filter.demographics.isNotEmpty()) criterion(Include(DEMOGRAPHIC, filter.demographics))
		if (validateYear(filter.yearFrom) || validateYear(filter.yearTo)) {
			criterion(QueryCriteria.Range(PUBLICATION_YEAR, filter.yearFrom, filter.yearTo))
		}
		if (validateYear(filter.year)) {
			criterion(Match(PUBLICATION_YEAR, filter.year))
		}
		filter.locale?.let {
			criterion(Include(LANGUAGE, setOf(it)))
		}
		filter.originalLocale?.let {
			criterion(Include(ORIGINAL_LANGUAGE, setOf(it)))
		}
		filter.query?.takeIf { it.isNotBlank() }?.let {
			criterion(Match(TITLE_NAME, it))
		}
	}.build()
}

/**
 * Converts a legacy [MangaSearchQuery] into a modern [MangaListFilter].
 *
 * Supported criteria:
 * - [QueryCriteria.Include] - Adds tags, states, content types, content ratings, demographics, and languages.
 * - [QueryCriteria.Exclude] - Excludes tags.
 * - [QueryCriteria.Range] - Sets a range of values like publication year range.
 * - [QueryCriteria.Match] - Adds search pattern for title name or exact publication year.
 *
 * @param searchQuery The legacy search query to convert.
 * @return A [MangaListFilter] populated with criteria from [searchQuery].
 * @throws IllegalArgumentException if an unsupported search field is encountered.
 */
public fun convertToMangaListFilter(searchQuery: MangaSearchQuery): MangaListFilter {
	return MangaListFilter.Builder().apply {
		for (criterion in searchQuery.criteria) {
			when (criterion) {
				is Include<*> -> handleInclude(this, criterion)
				is Exclude<*> -> handleExclude(this, criterion)
				is Range<*> -> handleBetween(this, criterion)
				is Match<*> -> handleMatch(this, criterion)
			}
		}
	}.build()
}

/**
 * Maps legacy [MangaSearchQueryCapabilities] to modern [MangaListFilterCapabilities].
 */
public fun MangaSearchQueryCapabilities.toMangaListFilterCapabilities(): MangaListFilterCapabilities = MangaListFilterCapabilities(
	isMultipleTagsSupported = capabilities.any { x -> x.field == TAG && x.isMultiple },
	isTagsExclusionSupported = capabilities.any { x -> x.field == TAG && x.criteriaTypes.contains(Exclude::class) },
	isSearchSupported = capabilities.any { x -> x.field == TITLE_NAME },
	isSearchWithFiltersSupported = capabilities.any { x -> x.field == TITLE_NAME && !x.isExclusive },
	isYearSupported = capabilities.any { x -> x.field == PUBLICATION_YEAR && x.criteriaTypes.contains(Match::class) },
	isYearRangeSupported = capabilities.any { x -> x.field == PUBLICATION_YEAR && x.criteriaTypes.contains(Range::class) },
	isOriginalLocaleSupported = capabilities.any { x -> x.field == ORIGINAL_LANGUAGE },
	isAuthorSearchSupported = capabilities.any { x -> x.field == AUTHOR },
)

/**
 * Maps modern [MangaListFilterCapabilities] back to legacy [MangaSearchQueryCapabilities].
 */
public fun MangaListFilterCapabilities.toMangaSearchQueryCapabilities(): MangaSearchQueryCapabilities =
	MangaSearchQueryCapabilities(
		capabilities = setOfNotNull(
			isMultipleTagsSupported.takeIf { it }?.let {
				SearchCapability(
					field = TAG,
					criteriaTypes = setOf(Include::class),
					isMultiple = true,
				)
			},
			isTagsExclusionSupported.takeIf { it }?.let {
				SearchCapability(
					field = TAG,
					criteriaTypes = setOf(Exclude::class),
					isMultiple = true,
				)
			},
			isSearchSupported.takeIf { it }?.let {
				SearchCapability(
					field = TITLE_NAME,
					criteriaTypes = setOf(Match::class),
					isMultiple = false,
					isExclusive = true,
				)
			},
			isSearchWithFiltersSupported.takeIf { it }?.let {
				SearchCapability(
					field = TITLE_NAME,
					criteriaTypes = setOf(Match::class),
					isMultiple = false,
				)
			},
			isYearSupported.takeIf { it }?.let {
				SearchCapability(
					field = PUBLICATION_YEAR,
					criteriaTypes = setOf(Match::class),
					isMultiple = false,
				)
			},
			isYearRangeSupported.takeIf { it }?.let {
				SearchCapability(
					field = PUBLICATION_YEAR,
					criteriaTypes = setOf(Range::class),
					isMultiple = false,
				)
			},
			isOriginalLocaleSupported.takeIf { it }?.let {
				SearchCapability(
					field = ORIGINAL_LANGUAGE,
					criteriaTypes = setOf(Include::class),
					isMultiple = true,
				)
			},
			SearchCapability(
				field = LANGUAGE,
				criteriaTypes = setOf(Include::class),
				isMultiple = true,
			),
			SearchCapability(
				field = STATE, criteriaTypes = setOf(Include::class), isMultiple = true,
			),
			SearchCapability(
				field = CONTENT_TYPE,
				criteriaTypes = setOf(Include::class),
				isMultiple = true,
			),
			SearchCapability(
				field = CONTENT_RATING,
				criteriaTypes = setOf(Include::class),
				isMultiple = true,
			),
			SearchCapability(
				field = DEMOGRAPHIC,
				criteriaTypes = setOf(Include::class),
				isMultiple = true,
			),
		),
	)

/**
 * Dispatches an [Include] criterion and sets corresponding tags, states, types, demographics, or locales on [builder].
 */
private fun handleInclude(builder: MangaListFilter.Builder, criterion: Include<*>) {
	val type = criterion.field.type

	when (criterion.field) {
		TAG -> builder.addTags(filterValues(criterion, type))
		STATE -> builder.addStates(filterValues(criterion, type))
		CONTENT_TYPE -> builder.addTypes(filterValues(criterion, type))
		CONTENT_RATING -> builder.addContentRatings(filterValues(criterion, type))
		DEMOGRAPHIC -> builder.addDemographics(filterValues(criterion, type))
		LANGUAGE -> builder.locale(getFirstValue(criterion, type))
		ORIGINAL_LANGUAGE -> builder.originalLocale(getFirstValue(criterion, type))
		else -> throw IllegalArgumentException("Unsupported field for Include criterion: ${criterion.field}")
	}
}

/**
 * Dispatches an [Exclude] criterion and configures excluded tags on [builder].
 */
private fun handleExclude(builder: MangaListFilter.Builder, criterion: Exclude<*>) {
	val type = criterion.field.type

	when (criterion.field) {
		TAG -> builder.excludeTags(filterValues(criterion, type))
		else -> throw IllegalArgumentException("Unsupported field for Exclude criterion: ${criterion.field}")
	}
}

/**
 * Dispatches a [Range] criterion (e.g. publication year range) and sets bounds on [builder].
 */
private fun handleBetween(builder: MangaListFilter.Builder, criterion: Range<*>) {
	val type = criterion.field.type

	when (criterion.field) {
		PUBLICATION_YEAR -> {
			builder.yearFrom(getValue(criterion.from, type, YEAR_UNKNOWN))
			builder.yearTo(getValue(criterion.to, type, YEAR_UNKNOWN))
		}

		else -> throw IllegalArgumentException("Unsupported field for Between criterion: ${criterion.field}")
	}
}

/**
 * Dispatches a [Match] criterion (e.g. title text query or exact publication year) on [builder].
 */
private fun handleMatch(builder: MangaListFilter.Builder, criterion: Match<*>) {
	val type = criterion.field.type

	when (criterion.field) {
		TITLE_NAME -> builder.query(getValue(criterion.value, type, ""))
		PUBLICATION_YEAR -> builder.year(getValue(criterion.value, type, YEAR_UNKNOWN))
		else -> throw IllegalArgumentException("Unsupported field for Match criterion: ${criterion.field}")
	}
}

/**
 * Filters and casts criterion values matching the expected runtime [type].
 */
@Suppress("UNCHECKED_CAST")
private fun <T> filterValues(criterion: Include<*>, type: Class<*>): List<T> {
	val values = criterion.values
	if (values.isEmpty()) return emptyList()
	return values.filter { type.isInstance(it) } as List<T>
}

/**
 * Filters and casts excluded criterion values matching the expected runtime [type].
 */
@Suppress("UNCHECKED_CAST")
private fun <T> filterValues(criterion: Exclude<*>, type: Class<*>): List<T> {
	val values = criterion.values
	if (values.isEmpty()) return emptyList()
	return values.filter { type.isInstance(it) } as List<T>
}

/**
 * Extracts and casts the first value from an [Include] criterion matching the expected [type].
 */
@Suppress("UNCHECKED_CAST")
private fun <T> getFirstValue(criterion: Include<*>, type: Class<*>): T? {
	return criterion.values.firstOrNull { type.isInstance(it) } as? T
}

/**
 * Coerces [value] to type [T] if it matches [type] or common boxed numeric/boolean primitives, or returns [default].
 */
@Suppress("UNCHECKED_CAST")
private fun <T> getValue(value: Any?, type: Class<*>, default: T): T {
	if (value == null) return default
	val isCompatible = type.isInstance(value)
		|| (type == Int::class.java && value is Int)
		|| (type == Long::class.java && value is Long)
		|| (type == Float::class.java && value is Float)
		|| (type == Double::class.java && value is Double)
		|| (type == Boolean::class.java && value is Boolean)

	return if (isCompatible) value as T else default
}

/**
 * Returns `true` if [year] is a valid known publication year.
 */
private fun validateYear(year: Int) = year != YEAR_UNKNOWN
