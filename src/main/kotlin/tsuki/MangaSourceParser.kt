package tsuki

import tsuki.model.ContentType

/**
 * Mandatory metadata annotation applied to each [MangaParser] class.
 *
 * This annotation is processed at compile-time by annotation processors / code generators
 * to discover parser implementations, register [tsuki.model.MangaSource] entries, and generate
 * the master catalog of available manga providers.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
public annotation class MangaSourceParser(
	/**
	 * Unique internal identifier of the manga source.
	 * Must be in `UPPER_CASE` format (e.g. `"MANGA_DEX"`, `"ASURA_SCANS"`), without spaces.
	 */
	val name: String,
	/**
	 * User-friendly display title of the manga source shown in the app UI (e.g. `"MangaDex"`, `"Asura Scans"`).
	 */
	val title: String,
	/**
	 * Two-letter ISO 639-1 language code (e.g. `"en"`, `"fr"`, `"ja"`, `"id"`), or blank (`""`)
	 * if the source hosts multilingual content across multiple languages.
	 */
	val locale: String = "",
	/**
	 * Primary category of content provided by this source.
	 * Defaults to [ContentType.MANGA].
	 *
	 * @see ContentType
	 */
	val type: ContentType = ContentType.MANGA,
)
