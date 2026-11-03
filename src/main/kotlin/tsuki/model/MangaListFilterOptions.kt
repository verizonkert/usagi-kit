package tsuki.model

import tsuki.InternalParsersApi
import java.util.Locale

/**
 * Describes the concrete set of available filter options and values supported by a manga source.
 *
 * Returned by parsers to populate filter dropdowns and tag selector chips in the client application.
 *
 * For optimal runtime performance and memory efficiency, prefer using [java.util.EnumSet]
 * when defining sets with more than one enum item (e.g. states, ratings, types, demographics).
 *
 * @property availableTags All genre/content tags available for this source.
 * @property availableStates Supported [MangaState] status filters (e.g. Ongoing, Finished).
 * @property availableContentRating Supported [ContentRating] maturity filters (e.g. Safe, Suggestive, Adult).
 * @property availableContentTypes Supported [ContentType] media format filters (e.g. Manga, Manhwa, Novel).
 * @property availableDemographics Supported [Demographic] audience filters (e.g. Shounen, Seinen).
 * @property availableLocales Supported language/translation locales for multilingual sources.
 */
public data class MangaListFilterOptions @InternalParsersApi constructor(
	public val availableTags: Set<MangaTag> = emptySet(),
	public val availableStates: Set<MangaState> = emptySet(),
	public val availableContentRating: Set<ContentRating> = emptySet(),
	public val availableContentTypes: Set<ContentType> = emptySet(),
	public val availableDemographics: Set<Demographic> = emptySet(),
	public val availableLocales: Set<Locale> = emptySet(),
)


