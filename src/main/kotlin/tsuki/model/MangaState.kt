package tsuki.model

/**
 * Publication and serialization status of a manga series.
 */
public enum class MangaState {
	/** Currently releasing new chapters on a regular basis. */
	ONGOING,

	/** Completed; all chapters and volumes have been released. */
	FINISHED,

	/** Abandoned or axed/cancelled prior to natural completion. */
	ABANDONED,

	/** Temporarily on hiatus or suspended by the author or publisher. */
	PAUSED,

	/** Announced or licensed, but serialization has not yet commenced. */
	UPCOMING,

	/** Restricted or suppressed due to licensing disputes, DMCA, or copyright claims. */
	RESTRICTED
}

