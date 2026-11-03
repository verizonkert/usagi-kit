package tsuki.model

/**
 * Maturity and age restriction classification for manga titles and parsers.
 */
public enum class ContentRating {
	/** Safe for all audiences with no explicit or suggestive content. */
	SAFE,

	/** Contains mild fanservice, ecchi elements, or mildly suggestive themes. */
	SUGGESTIVE,

	/** Explicit adult content, hentai, or graphic mature themes restricted to adult audiences. */
	ADULT
}

