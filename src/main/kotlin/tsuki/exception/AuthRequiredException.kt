package tsuki.exception

import okio.IOException
import tsuki.InternalParsersApi
import tsuki.model.MangaSource

/**
 * Thrown when access to requested manga content or API endpoints requires active user authentication/login.
 *
 * When caught by the host application, this exception signals the UI to prompt the user to log in
 * via the source's authorization provider or Web login interface.
 *
 * @property source The [MangaSource] that requires user credentials.
 * @param cause Optional underlying cause.
 */
public class AuthRequiredException @InternalParsersApi @JvmOverloads constructor(
	public val source: MangaSource,
	cause: Throwable? = null,
) : IOException("Authorization required for source '${source.name}'", cause)
