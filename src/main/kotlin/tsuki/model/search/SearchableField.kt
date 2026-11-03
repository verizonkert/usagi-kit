package tsuki.model.search

import tsuki.model.ContentRating
import tsuki.model.ContentType
import tsuki.model.Demographic
import tsuki.model.MangaState
import tsuki.model.MangaTag
import java.util.Locale

/**
 * Represents the various fields that can be used for searching and filtering manga.
 *
 * Each field is associated with a specific data type [type] defining the expected value classes.
 *
 * @property type The Java [Class] representing the expected runtime type of values for this field.
 */
@Deprecated(
	message = "MangaSearchQuery and SearchableField are deprecated in favor of MangaListFilter.",
	replaceWith = ReplaceWith("MangaListFilter", "tsuki.model.MangaListFilter"),
)
public enum class SearchableField(public val type: Class<*>) {
	/** Title keyword or title name search field (values of type [String]). */
	TITLE_NAME(String::class.java),

	/** Genre or content tag filter field (values of type [MangaTag]). */
	TAG(MangaTag::class.java),

	/** Author or artist name filter field (values of type [MangaTag]). */
	AUTHOR(MangaTag::class.java),

	/** Translation or target language filter field (values of type [Locale]). */
	LANGUAGE(Locale::class.java),

	/** Original publication language filter field (values of type [Locale]). */
	ORIGINAL_LANGUAGE(Locale::class.java),

	/** Publication or scanlation state filter field (values of type [MangaState]). */
	STATE(MangaState::class.java),

	/** Content medium type filter field, such as Manga, Manhwa, or Novel (values of type [ContentType]). */
	CONTENT_TYPE(ContentType::class.java),

	/** Content maturity and age rating filter field (values of type [ContentRating]). */
	CONTENT_RATING(ContentRating::class.java),

	/** Demographic audience filter field, such as Shounen or Seinen (values of type [Demographic]). */
	DEMOGRAPHIC(Demographic::class.java),

	/** Publication year filter field (values of boxed type [Int]). */
	PUBLICATION_YEAR(Int::class.javaObjectType);
}

