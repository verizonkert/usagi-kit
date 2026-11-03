@file:Suppress("DEPRECATION")

package tsuki.core

import tsuki.MangaParser
import tsuki.MangaParserAuthProvider

/**
 * Decorator base class that wraps a [delegate] [MangaParser] and forwards all operations to it.
 *
 * Can be subclassed to intercept, transform, or enrich parser calls (such as adding caching or logging).
 *
 * @param delegate The underlying [MangaParser] being decorated.
 */
public open class MangaParserWrapper(
	public val delegate: MangaParser,
) : MangaParser by delegate, MangaParserAuthProvider by (delegate.authorizationProvider ?: EmptyAuthProvider) {

	/**
	 * Accurately reflects whether the underlying [delegate] supports authentication.
	 * Prevents returning a dummy provider when the delegate does not support login.
	 */
	override val authorizationProvider: MangaParserAuthProvider?
		get() = delegate.authorizationProvider

	/**
	 * Recursively unwraps this wrapper to find the innermost [MangaParser] instance.
	 */
	public fun unwrap(): MangaParser {
		var current: MangaParser = delegate
		while (current is MangaParserWrapper) {
			current = current.delegate
		}
		return current
	}

	/**
	 * Checks and casts this wrapper to a parser of type [T].
	 */
	public inline fun <reified T : MangaParser> unwrapAs(): T? = unwrap() as? T

	private object EmptyAuthProvider : MangaParserAuthProvider {
		override val authUrl: String = ""
		override suspend fun isAuthorized(): Boolean = true
	}
}
