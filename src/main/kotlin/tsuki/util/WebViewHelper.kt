@file:Suppress("warnings", "unused")

package tsuki.util

import tsuki.MangaLoaderContext

/**
 * Helper utility for extracting browser-managed state (such as local storage values or URL query parameters)
 * by delegating script evaluation to [MangaLoaderContext].
 *
 * @param context The loader context capable of evaluating JavaScript within a target domain's origin.
 */
public class WebViewHelper(
	private val context: MangaLoaderContext,
) {

	/**
	 * Reads an item from `window.localStorage` within the origin context of [domain].
	 *
	 * Properly escapes quotes and backslashes in [key] to prevent script injection.
	 *
	 * @param domain Target website domain.
	 * @param key The local storage item key name.
	 * @return The stored string value, or `null` if the key does not exist or evaluation failed.
	 */
	public suspend fun getLocalStorageValue(domain: String, key: String): String? {
		val cleanDomain = domain.removePrefix("https://").removePrefix("http://").substringBefore('/')
		val escapedKey = key.replace("\\", "\\\\").replace("\"", "\\\"")
		return context.evaluateJs("https://$cleanDomain/", "window.localStorage.getItem(\"$escapedKey\")")
	}

	/**
	 * Extracts a specific query parameter from the URL using the browser's `URLSearchParams` API.
	 *
	 * @param url The page URL to evaluate against.
	 * @param value The query parameter key name to inspect.
	 * @return The resolved query parameter value, or `null` if absent.
	 */
	public suspend fun getUrlValue(url: String, value: String): String? {
		val escapedValue = value.replace("\\", "\\\\").replace("'", "\\'")
		return context.evaluateJs(url, "new URLSearchParams(window.location.search).get('$escapedValue');")
	}
}
