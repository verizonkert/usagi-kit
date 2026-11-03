package tsuki

/**
 * Optional interface for manga source parsers requiring user authentication or login sessions.
 *
 * When a parser implements [MangaParserAuthProvider], the host application:
 * 1. Checks [isAuthorized] before requesting premium, restricted, or bookmark content.
 * 2. If unauthorized, displays a login action to the user that opens [authUrl] in an in-app WebView.
 * 3. Monitors cookies/tokens saved into [CookieJar] from that webview session.
 * 4. Displays [getUsername] in the app settings/source management screen when logged in.
 */
public interface MangaParserAuthProvider {

	/**
	 * Web URL of the login or authorization page loaded in the interactive WebView.
	 */
	public val authUrl: String

	/**
	 * Checks whether the user currently has an active, valid authentication session.
	 *
	 * Implementations typically verify whether a required authentication cookie or token
	 * exists in the [CookieJar], or send a lightweight profile verification ping.
	 *
	 * @return `true` if authorized, `false` otherwise.
	 */
	public suspend fun isAuthorized(): Boolean

	/**
	 * Returns the username or account identifier of the currently logged-in user,
	 * or `null` if unavailable or anonymous.
	 */
	public suspend fun getUsername(): String? = null
}
