package tsuki.model

import tsuki.InternalParsersApi

/**
 * Declares the search and filtering capabilities supported by a manga parser.
 *
 * Applications query this declaration to dynamically configure UI filters, disabling
 * unsupported controls (e.g. tag exclusion, year ranges, or language selectors).
 *
 * @property isMultipleTagsSupported Whether the parser supports filtering by more than one genre/tag simultaneously.
 * @property isTagsExclusionSupported Whether the parser supports tag exclusion ([MangaListFilter.tagsExclude]).
 * @property isSearchSupported Whether the parser supports text keyword searching ([MangaListFilter.query]).
 * @property isSearchWithFiltersSupported Whether keyword search can be combined with other filter parameters.
 * @property isYearSupported Whether filtering by a single release year is supported ([MangaListFilter.year]).
 * @property isYearRangeSupported Whether filtering by release year range is supported ([MangaListFilter.yearFrom], [MangaListFilter.yearTo]).
 * @property isOriginalLocaleSupported Whether filtering by original publication language is supported ([MangaListFilter.originalLocale]).
 * @property isAuthorSearchSupported Whether searching or filtering by author/artist name is supported ([MangaListFilter.author]).
 */
public data class MangaListFilterCapabilities @InternalParsersApi constructor(
	val isMultipleTagsSupported: Boolean = false,
	val isTagsExclusionSupported: Boolean = false,
	val isSearchSupported: Boolean = false,
	val isSearchWithFiltersSupported: Boolean = false,
	val isYearSupported: Boolean = false,
	val isYearRangeSupported: Boolean = false,
	val isOriginalLocaleSupported: Boolean = false,
	val isAuthorSearchSupported: Boolean = false,
)


