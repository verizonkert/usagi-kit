package tsuki.util

import okhttp3.HttpUrl
import org.jsoup.nodes.Element
import tsuki.ErrorMessages
import tsuki.InternalParsersApi
import tsuki.MangaParser
import tsuki.core.AbstractMangaParser
import tsuki.exception.ParseException
import tsuki.model.ContentRating
import tsuki.model.ContentType
import tsuki.model.Demographic
import tsuki.model.Manga
import tsuki.model.MangaChapter
import tsuki.model.MangaPage
import tsuki.model.MangaSource
import tsuki.model.MangaState
import tsuki.model.MangaTag

/**
 * Creates a stable 64-bit unique ID for a [Manga], [MangaChapter], or [MangaPage].
 *
 * Combines a seed hash, the source identifier name, and the relative URL using polynomial hashing.
 *
 * @param source The manga source descriptor.
 * @param url Relative URL path (without domain/protocol).
 * @return A deterministic 64-bit identifier.
 * @see Manga.id
 * @see MangaChapter.id
 * @see MangaPage.id
 */
public fun generateUid(source: MangaSource, url: String): Long {
	var h = LONG_HASH_SEED
	val sourceName = source.name
	for (i in 0 until sourceName.length) {
		h = 31 * h + sourceName[i].code
	}
	for (i in 0 until url.length) {
		h = 31 * h + url[i].code
	}
	return h
}

/**
 * Creates a stable 64-bit unique ID for a [Manga], [MangaChapter], or [MangaPage] from an internal numeric ID.
 *
 * @param source The manga source descriptor.
 * @param id Upstream source numeric identifier.
 * @return A deterministic 64-bit identifier.
 * @see Manga.id
 * @see MangaChapter.id
 * @see MangaPage.id
 */
public fun generateUid(source: MangaSource, id: Long): Long {
	var h = LONG_HASH_SEED
	val sourceName = source.name
	for (i in 0 until sourceName.length) {
		h = 31 * h + sourceName[i].code
	}
	h = 31 * h + id
	return h
}

/**
 * Throws a [ParseException] capturing the document URI and context of this HTML element.
 */
@InternalParsersApi
public fun Element.parseFailed(message: String? = null): Nothing {
	throw ParseException(message, ownerDocument()?.location() ?: baseUri(), null)
}

/**
 * Ensures at most one [MangaTag] is selected in this filter set, throwing if multiple are specified.
 */
@InternalParsersApi
public fun Set<MangaTag>?.oneOrThrowIfMany(): MangaTag? = oneOrThrowIfMany(
	ErrorMessages.FILTER_MULTIPLE_GENRES_NOT_SUPPORTED,
)

/**
 * Ensures at most one [MangaState] is selected in this filter set, throwing if multiple are specified.
 */
@InternalParsersApi
public fun Set<MangaState>?.oneOrThrowIfMany(): MangaState? = oneOrThrowIfMany(
	ErrorMessages.FILTER_MULTIPLE_STATES_NOT_SUPPORTED,
)

/**
 * Ensures at most one [ContentType] is selected in this filter set, throwing if multiple are specified.
 */
@InternalParsersApi
public fun Set<ContentType>?.oneOrThrowIfMany(): ContentType? = oneOrThrowIfMany(
	ErrorMessages.FILTER_MULTIPLE_CONTENT_TYPES_NOT_SUPPORTED,
)

/**
 * Ensures at most one [Demographic] is selected in this filter set, throwing if multiple are specified.
 */
@InternalParsersApi
public fun Set<Demographic>?.oneOrThrowIfMany(): Demographic? = oneOrThrowIfMany(
	ErrorMessages.FILTER_MULTIPLE_DEMOGRAPHICS_NOT_SUPPORTED,
)

/**
 * Ensures at most one [ContentRating] is selected in this filter set, throwing if multiple are specified.
 */
@InternalParsersApi
public fun Set<ContentRating>?.oneOrThrowIfMany(): ContentRating? = oneOrThrowIfMany(
	ErrorMessages.FILTER_MULTIPLE_CONTENT_RATING_NOT_SUPPORTED,
)

/**
 * Validates that this set contains at most one item, throwing an [IllegalArgumentException] with [msg] if size > 1.
 */
private fun <T> Set<T>?.oneOrThrowIfMany(msg: String): T? = when {
	isNullOrEmpty() -> null
	size == 1 -> first()
	else -> throw IllegalArgumentException(msg)
}

/**
 * Creates an HTTPS [HttpUrl.Builder] configured with the specified [domain] and optional [subdomain].
 */
public fun urlBuilder(domain: String, subdomain: String? = null): HttpUrl.Builder {
	val cleanDomain = domain.removePrefix("https://").removePrefix("http://").substringBefore('/')
	val host = if (subdomain == null) {
		cleanDomain
	} else {
		"$subdomain.${cleanDomain.removePrefix("www.")}"
	}
	return HttpUrl.Builder()
		.scheme(SCHEME_HTTPS)
		.host(host)
}

/**
 * Convenience extension for generating a unique 64-bit ID from a relative URL within this parser.
 */
public fun MangaParser.generateUid(url: String): Long =
	tsuki.util.generateUid(source, url)

/**
 * Convenience extension for generating a unique 64-bit ID from an upstream numeric ID within this parser.
 */
public fun MangaParser.generateUid(id: Long): Long =
	tsuki.util.generateUid(source, id)

/**
 * Creates an HTTPS [HttpUrl.Builder] pre-configured with this parser's active [MangaParser.domain].
 */
public fun MangaParser.urlBuilder(subdomain: String? = null): HttpUrl.Builder =
	tsuki.util.urlBuilder(domain, subdomain)

/**
 * Computes a fully qualified domain name given a parser and target subdomain.
 */
@InternalParsersApi
public fun getDomain(parser: MangaParser, subdomain: String): String {
	return subdomain + "." + parser.domain.removePrefix("www.")
}
