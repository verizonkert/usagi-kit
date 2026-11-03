package tsuki

import tsuki.exception.ParseException

/**
 * Standard error messages, common failure constants, and validation helper functions
 * utilized across manga parsers and network clients.
 *
 * This object preserves all legacy constant symbols required by compiled extensions and plugins
 * while providing unified factory routines to construct consistent exception instances.
 */
public object ErrorMessages {

    // --- Standard Filter Error Constants (Preserved for ABI compatibility) ---

    /**
     * Error message displayed when a parser receives multiple publication state filters,
     * but only supports querying a single state at a time.
     */
    public const val FILTER_MULTIPLE_STATES_NOT_SUPPORTED: String =
        "Multiple states are not supported by this source"

    /**
     * Error message displayed when a parser receives multiple genres/tags,
     * but the upstream source API only accepts a single genre parameter.
     */
    public const val FILTER_MULTIPLE_GENRES_NOT_SUPPORTED: String =
        "Multiple genres are not supported by this source"

    /**
     * Error message displayed when multiple content ratings (e.g. SAFE and NSFW)
     * are supplied to a source that cannot query both simultaneously.
     */
    public const val FILTER_MULTIPLE_CONTENT_RATING_NOT_SUPPORTED: String =
        "Multiple Content ratings are not supported by this source"

    /**
     * Error message displayed when multiple content types (e.g. MANGA and NOVEL)
     * are combined in a filter against an incompatible source.
     */
    public const val FILTER_MULTIPLE_CONTENT_TYPES_NOT_SUPPORTED: String =
        "Multiple Content types are not supported by this source"

    /**
     * Error message displayed when filtering by more than one demographic target
     * (e.g. Shounen and Seinen) is unsupported by the source.
     */
    public const val FILTER_MULTIPLE_DEMOGRAPHICS_NOT_SUPPORTED: String =
        "Multiple Demographics are not supported by this source"

    /**
     * Error message displayed when a source does not allow combining language/locale
     * constraints with tag/genre filters.
     */
    public const val FILTER_BOTH_LOCALE_GENRES_NOT_SUPPORTED: String =
        "Filtering by both genres and locale is not supported by this source"

    /**
     * Error message displayed when a source cannot combine publication state filters
     * with tag/genre filters simultaneously.
     */
    public const val FILTER_BOTH_STATES_GENRES_NOT_SUPPORTED: String =
        "Filtering by both genres and states is not supported by this source"

    /**
     * Error message displayed when search queries are dispatched to a source
     * that does not implement search capability.
     */
    public const val SEARCH_NOT_SUPPORTED: String =
        "Search is not supported by this source"

    // --- Common Network, Parsing, and Extraction Constants ---

    /**
     * Error message returned when an HTTP response body is empty or null.
     */
    public const val EMPTY_RESPONSE: String = "Received an empty or null response from the server"

    /**
     * Error message returned when no chapter entries could be extracted from a title's page.
     */
    public const val CHAPTERS_NOT_FOUND: String = "No chapters could be discovered for this title"

    /**
     * Error message returned when a chapter does not contain any readable page items.
     */
    public const val PAGES_NOT_FOUND: String = "No pages found for this chapter"

    /**
     * Error message returned when network requests are interrupted by Cloudflare antibot challenges.
     */
    public const val CLOUDFLARE_BLOCKED: String = "Request was blocked by Cloudflare verification"

    /**
     * Error message returned when an unexpected DOM or JSON structure causes a parsing failure.
     */
    public const val PARSE_FAILED: String = "Failed to parse document structure"

    // --- Dynamic Formatter Functions ---

    /**
     * Formats an informative error message explaining that a specific filter is not supported.
     *
     * @param filterName Name of the filter that was attempted (e.g., "Year", "Author").
     * @param sourceName Optional display name of the manga source.
     * @return Formatted error string.
     */
    @JvmStatic
    public fun unsupportedFilter(filterName: String, sourceName: String? = null): String {
        return if (sourceName != null) {
            "Filter '$filterName' is not supported by source '$sourceName'"
        } else {
            "Filter '$filterName' is not supported by this source"
        }
    }

    /**
     * Formats an HTTP failure description incorporating response code and endpoint URL.
     *
     * @param code HTTP status code returned by the server (e.g., 403, 404, 500).
     * @param url Optional target endpoint URL where the error occurred.
     * @return Formatted error string.
     */
    @JvmStatic
    public fun httpError(code: Int, url: String? = null): String {
        return if (url != null) {
            "HTTP request failed with status code $code for URL: $url"
        } else {
            "HTTP request failed with status code $code"
        }
    }

    /**
     * Factory function that constructs a standardized [ParseException].
     *
     * @param target Description of the target entity being parsed (e.g., "chapter list", "cover image").
     * @param details Optional supplementary technical details or upstream message.
     * @param url The URL where the parsing failure took place, defaulting to an empty string.
     * @return An initialized [ParseException] ready to be thrown.
     */
    @JvmStatic
    @JvmOverloads
    public fun parsingError(target: String, details: String? = null, url: String = ""): ParseException {
        val msg = if (details.isNullOrBlank()) {
            "Failed to parse $target"
        } else {
            "Failed to parse $target: $details"
        }
        return ParseException(msg, url)
    }

    // --- Validation Helper ---

    /**
     * Validates that an optional collection contains at most one element.
     *
     * If the collection is null or empty, returns null. If it contains a single item, returns that item.
     * If it contains more than one item, throws an [IllegalArgumentException] with [errorMessage].
     *
     * @param items Collection of items to validate.
     * @param errorMessage Error message to throw if more than one element is present.
     * @return The single element present, or null if the collection is empty.
     * @throws IllegalArgumentException if the collection has more than 1 item.
     */
    @JvmStatic
    public fun <T> requireSingle(items: Collection<T>?, errorMessage: String): T? {
        if (items.isNullOrEmpty()) return null
        require(items.size <= 1) { errorMessage }
        return items.first()
    }
}
