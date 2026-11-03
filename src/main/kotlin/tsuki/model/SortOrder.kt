package tsuki.model

/**
 * Sorting strategies for querying and browsing manga catalogs and search listings.
 */
public enum class SortOrder {
	/** Recently updated entries first (descending). */
	UPDATED,

	/** Least recently updated entries first (ascending). */
	UPDATED_ASC,

	/** Highest overall popularity first. */
	POPULARITY,

	/** Lowest overall popularity first. */
	POPULARITY_ASC,

	/** Highest rated / highest score first. */
	RATING,

	/** Lowest rated / lowest score first. */
	RATING_ASC,

	/** Newly added entries first (descending). */
	NEWEST,

	/** Oldest added entries first (ascending). */
	NEWEST_ASC,

	/** Alphabetical title order (A to Z). */
	ALPHABETICAL,

	/** Reverse alphabetical title order (Z to A). */
	ALPHABETICAL_DESC,

	/** Most recently added to the site. */
	ADDED,

	/** Earliest added to the site. */
	ADDED_ASC,

	/** Most relevant to search keywords. */
	RELEVANCE,

	/** Most popular in the last hour. */
	POPULARITY_HOUR,

	/** Most popular today (last 24 hours). */
	POPULARITY_TODAY,

	/** Most popular this week (last 7 days). */
	POPULARITY_WEEK,

	/** Most popular this month (last 30 days). */
	POPULARITY_MONTH,

	/** Most popular this year (last 365 days). */
	POPULARITY_YEAR,
}

