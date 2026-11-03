package tsuki.exception

/**
 * Thrown when requested manga content (such as a specific title, chapter, or image set)
 * is temporarily or permanently unavailable (e.g. DMCA takedowns, region blocks, or server-side paywalls).
 *
 * @param message Descriptive explanation of why the content is unavailable.
 * @param cause Optional underlying cause.
 */
public class ContentUnavailableException @JvmOverloads constructor(
	message: String,
	cause: Throwable? = null,
) : RuntimeException(message, cause)
