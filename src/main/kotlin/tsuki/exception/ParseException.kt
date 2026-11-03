package tsuki.exception

import tsuki.InternalParsersApi

/**
 * Thrown when an HTML document, JSON payload, or web response cannot be parsed or lacks mandatory elements.
 *
 * Captures both the contextual error message and the target [url] where parsing failed.
 *
 * @property shortMessage Brief error description without the URL prefix.
 * @property url The URL of the page or document where the parse failure occurred.
 * @param cause Optional underlying cause.
 */
public class ParseException @InternalParsersApi @JvmOverloads constructor(
	public val shortMessage: String?,
	public val url: String,
	cause: Throwable? = null,
) : RuntimeException(if (shortMessage != null) "$shortMessage at $url" else "Parse failed at $url", cause)
