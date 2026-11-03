package tsuki.network

/**
 * Curated collection of modern and legacy User-Agent strings.
 *
 * Used across parsers to emulate common browsers (Chrome, Firefox) on Android,
 * Linux, and Windows platforms to bypass simplistic User-Agent checks or access
 * mobile-optimized API endpoints.
 */
public object UserAgents {

	/** Modern Google Chrome User-Agent string for Android mobile devices. */
	public const val CHROME_MOBILE: String =
		"Mozilla/5.0 (Linux; Android 16) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Mobile Safari/537.36"

	/** Modern Mozilla Firefox User-Agent string for Android mobile devices. */
	public const val FIREFOX_MOBILE: String =
		"Mozilla/5.0 (Android 16; Mobile; LG-M255; rv:146.0) Gecko/146.0 Firefox/146.0"

	/* Fallback: Set Linux as default for "DESKTOP" User-Agent */

	/** Modern Google Chrome User-Agent string for Linux desktop environments. */
	public const val CHROME_DESKTOP: String =
		"Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.7444.59 Safari/537.36"

	/** Modern Mozilla Firefox User-Agent string for Linux desktop environments. */
	public const val FIREFOX_DESKTOP: String = "Mozilla/5.0 (X11; Linux x86_64; rv:145.0) Gecko/20100101 Firefox/145.0"

	/** Modern Google Chrome User-Agent string for Windows 64-bit desktop environments. */
	public const val CHROME_WINDOWS: String =
		"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36"

	/** Modern Mozilla Firefox User-Agent string for Windows 64-bit desktop environments. */
	public const val FIREFOX_WINDOWS: String =
		"Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:148.0) Gecko/20100101 Firefox/148.0"

	/** Default Kotatsu application User-Agent header string. */
	public const val KOTATSU: String = "Kotatsu/9.0 (Android 16;;; en)"

	/** Legacy Kotatsu application User-Agent header string. */
	public const val LEGACY_KOTATSU: String = "Kotatsu/6.8 (Android 13;;; en)"
}

