package tsuki.exception

import okio.IOException
import tsuki.InternalParsersApi
import tsuki.model.MangaSource

/**
 * Thrown when access to requested content requires user interactive input (such as entering a password,
 * an invite code, or a personal access token).
 *
 * When the app catches this exception, it prompts the user with a dialog displaying [message].
 * After the user confirms, the entered text is stored under [key] so that the parser can retrieve it
 * on the subsequent attempt.
 *
 * @property source The [MangaSource] requesting user input.
 * @property message A human-readable prompt displayed inside the input dialog.
 * @property key Key identifier used by the parser to retrieve the stored input from configuration.
 * @param cause Optional underlying cause.
 */
public class InputRequiredException @InternalParsersApi @JvmOverloads constructor(
	public val source: MangaSource,
	override val message: String,
	public val key: String = "",
	cause: Throwable? = null,
) : IOException(message, cause)
