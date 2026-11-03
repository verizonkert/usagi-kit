package tsuki.model

/**
 * Target demographic audience classification for manga series.
 */
public enum class Demographic {
	/** Targeted primarily at young male audiences. */
	SHOUNEN,

	/** Targeted primarily at young female audiences. */
	SHOUJO,

	/** Targeted primarily at adult men (18+). */
	SEINEN,

	/** Targeted primarily at adult women (18+). */
	JOSEI,

	/** Targeted primarily at young children. */
	KODOMO,

	/** No specific target demographic audience declared. */
	NONE,
}

