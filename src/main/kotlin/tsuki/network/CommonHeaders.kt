package tsuki.network

/**
 * Standard HTTP header name constants used across manga parsers and network interceptors.
 *
 * Provides centralized header string symbols to avoid typo bugs and ensure consistency
 * across network requests, User-Agent overrides, referer policies, and authentication headers.
 */
public object CommonHeaders {

	/** Standard HTTP `Referer` header. */
	const val REFERER: String = "Referer"
	/** Standard HTTP `User-Agent` header. */
	const val USER_AGENT: String = "User-Agent"
	/** Standard HTTP `Accept` header. */
	const val ACCEPT: String = "Accept"
	/** Standard HTTP `Accept-Language` header. */
	const val ACCEPT_LANGUAGE: String = "Accept-Language"
	/** Standard HTTP `Accept-Charset` header. */
	const val ACCEPT_CHARSET: String = "Accept-Charset"
	/** Standard HTTP `Accept-Encoding` header. */
	const val ACCEPT_ENCODING: String = "Accept-Encoding"

	/** Standard HTTP `Content-Type` entity header. */
	const val CONTENT_TYPE: String = "Content-Type"
	/** Standard HTTP `Content-Length` entity header. */
	const val CONTENT_LENGTH: String = "Content-Length"
	/** Standard HTTP `Content-Disposition` entity header. */
	const val CONTENT_DISPOSITION: String = "Content-Disposition"
	/** Standard HTTP `Content-Encoding` entity header. */
	const val CONTENT_ENCODING: String = "Content-Encoding"
	/** Standard HTTP `Content-Language` entity header. */
	const val CONTENT_LANGUAGE: String = "Content-Language"

	/** Standard HTTP `Authorization` request header. */
	const val AUTHORIZATION: String = "Authorization"
	/** Standard HTTP `Proxy-Authorization` request header. */
	const val PROXY_AUTHORIZATION: String = "Proxy-Authorization"

	/** Standard HTTP `Cookie` request header. */
	const val COOKIE: String = "Cookie"
	/** Standard HTTP `Set-Cookie` response header. */
	const val SET_COOKIE: String = "Set-Cookie"

	/** Standard HTTP `Cache-Control` header. */
	const val CACHE_CONTROL: String = "Cache-Control"
	/** Standard HTTP `Pragma` legacy cache header. */
	const val PRAGMA: String = "Pragma"
	/** Standard HTTP `Expires` caching header. */
	const val EXPIRES: String = "Expires"

	/** Standard HTTP conditional validation header `Last-Modified`. */
	const val LAST_MODIFIED: String = "Last-Modified"
	/** Standard HTTP conditional validation header `If-Modified-Since`. */
	const val IF_MODIFIED_SINCE: String = "If-Modified-Since"
	/** Standard HTTP conditional validation header `ETag`. */
	const val ETAG: String = "ETag"
	/** Standard HTTP conditional validation header `If-None-Match`. */
	const val IF_NONE_MATCH: String = "If-None-Match"

	/** Standard HTTP redirect target header `Location`. */
	const val LOCATION: String = "Location"
	/** Standard HTTP target host header `Host`. */
	const val HOST: String = "Host"
	/** Standard CORS request header `Origin`. */
	const val ORIGIN: String = "Origin"
	/** Standard HTTP connection persistence header `Connection`. */
	const val CONNECTION: String = "Connection"

	/** Standard HTTP byte range request header `Range`. */
	const val RANGE: String = "Range"
	/** Standard HTTP byte range response header `Content-Range`. */
	const val CONTENT_RANGE: String = "Content-Range"

	/** Standard HTTP backoff header `Retry-After`. */
	const val RETRY_AFTER: String = "Retry-After"
	/** Standard HTTP software identification header `Server`. */
	const val SERVER: String = "Server"
	/** Standard HTTP message origination timestamp header `Date`. */
	const val DATE: String = "Date"

	/** Proxy forwarding header `X-Forwarded-For`. */
	const val X_FORWARDED_FOR: String = "X-Forwarded-For"
	/** Proxy client identification header `X-Real-IP`. */
	const val X_REAL_IP: String = "X-Real-IP"
	/** AJAX / XMLHttpRequest identifier header `X-Requested-With`. */
	const val X_REQUESTED_WITH: String = "X-Requested-With"

	/* Fetch metadata headers */
	/** W3C Fetch Metadata destination indicator `Sec-Fetch-Dest`. */
	const val SEC_FETCH_DEST: String = "Sec-Fetch-Dest"
	/** W3C Fetch Metadata request mode `Sec-Fetch-Mode`. */
	const val SEC_FETCH_MODE: String = "Sec-Fetch-Mode"
	/** W3C Fetch Metadata relationship site `Sec-Fetch-Site`. */
	const val SEC_FETCH_SITE: String = "Sec-Fetch-Site"
	/** W3C Fetch Metadata user-activation flag `Sec-Fetch-User`. */
	const val SEC_FETCH_USER: String = "Sec-Fetch-User"

	/* Client hints headers */
	/** User-Agent client hint brand and version `Sec-Ch-Ua`. */
	const val SEC_CH_UA: String = "Sec-Ch-Ua"
	/** User-Agent client hint mobile device flag `Sec-Ch-Ua-Mobile`. */
	const val SEC_CH_UA_MOBILE: String = "Sec-Ch-Ua-Mobile"
	/** User-Agent client hint OS platform `Sec-Ch-Ua-Platform`. */
	const val SEC_CH_UA_PLATFORM: String = "Sec-Ch-Ua-Platform"

	/* Client API headers */
	/** Custom client timestamp header `x-client-ts`. */
	const val X_CLIENT_TS: String = "x-client-ts"
	/** Custom client signature header `x-client-sig`. */
	const val X_CLIENT_SIG: String = "x-client-sig"

	/* API specific headers */
	/** Application key identifier header `x-app-key`. */
	const val X_APP_KEY: String = "x-app-key"
	/** Application origin header `x-app-origin`. */
	const val X_APP_ORIGIN: String = "x-app-origin"
	/** API request timestamp header `x-wm-request-time`. */
	const val X_WM_REQUEST_TIME: String = "x-wm-request-time"
	/** API access key header `x-wm-accses-key`. */
	const val X_WM_ACCESS_KEY: String = "x-wm-accses-key"
	/** API request signature header `x-wm-request-signature`. */
	const val X_WM_REQUEST_SIGNATURE: String = "x-wm-request-signature"

	/* Other browser headers */
	/** HTTPS upgrade preference header `Upgrade-Insecure-Requests`. */
	const val UPGRADE_INSECURE_REQUESTS: String = "Upgrade-Insecure-Requests"

	/* Some custom headers */
	/** Custom parser metadata header indicating source key `X-Manga-Source`. */
	const val MANGA_SOURCE: String = "X-Manga-Source"
	/** General bearer or session authentication header `Token`. */
	const val TOKEN: String = "Token"
}

