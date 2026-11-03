package tsuki.network

import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Response
import org.json.JSONObject

/**
 * High-level coroutine-based HTTP and GraphQL client interface for manga parsers.
 *
 * Provides suspending methods for common HTTP operations (GET, HEAD, POST with form/payload/JSON)
 * and GraphQL requests with automatic error unwrapping and status validation.
 */
public interface WebClient {

	/**
	 * Performs a suspending HTTP `GET` request to the specified string [url].
	 *
	 * @param url The target URL string.
	 * @return The successful HTTP [Response].
	 */
	public suspend fun httpGet(url: String): Response = httpGet(url.toHttpUrl())

	/**
	 * Performs a suspending HTTP `GET` request to the specified string [url] with additional [extraHeaders].
	 *
	 * @param url The target URL string.
	 * @param extraHeaders Optional request headers merged into the request.
	 * @return The successful HTTP [Response].
	 */
	public suspend fun httpGet(url: String, extraHeaders: Headers?): Response = httpGet(url.toHttpUrl(), extraHeaders)

	/**
	 * Performs a suspending HTTP `GET` request to the specified [url].
	 *
	 * @param url The target [HttpUrl].
	 * @return The successful HTTP [Response].
	 */
	public suspend fun httpGet(url: HttpUrl): Response = httpGet(url, null)

	/**
	 * Performs a suspending HTTP `GET` request to the specified [url] with optional [extraHeaders].
	 *
	 * @param url The target [HttpUrl].
	 * @param extraHeaders Optional request headers merged into the request.
	 * @return The successful HTTP [Response].
	 */
	public suspend fun httpGet(url: HttpUrl, extraHeaders: Headers?): Response

	/**
	 * Performs a suspending HTTP `HEAD` request to the specified string [url].
	 *
	 * Useful for lightweight header inspection or checking file existence without downloading the body.
	 *
	 * @param url The target URL string.
	 * @return The successful HTTP [Response] without response body.
	 */
	public suspend fun httpHead(url: String): Response = httpHead(url.toHttpUrl())

	/**
	 * Performs a suspending HTTP `HEAD` request to the specified [url].
	 *
	 * @param url The target [HttpUrl].
	 * @return The successful HTTP [Response] without response body.
	 */
	public suspend fun httpHead(url: HttpUrl): Response

	/**
	 * Performs a suspending HTTP `POST` request to [url] with form-url-encoded key-value pairs.
	 *
	 * @param url The target URL string.
	 * @param form Map of form field keys and values.
	 * @return The successful HTTP [Response].
	 */
	public suspend fun httpPost(url: String, form: Map<String, String>): Response =
		httpPost(url.toHttpUrl(), form, null)

	/**
	 * Performs a suspending HTTP `POST` request to [url] with form-url-encoded key-value pairs.
	 *
	 * @param url The target [HttpUrl].
	 * @param form Map of form field keys and values.
	 * @return The successful HTTP [Response].
	 */
	public suspend fun httpPost(url: HttpUrl, form: Map<String, String>): Response = httpPost(url, form, null)

	/**
	 * Performs a suspending HTTP `POST` request to [url] with form-url-encoded key-value pairs
	 * and optional [extraHeaders].
	 *
	 * @param url The target [HttpUrl].
	 * @param form Map of form field keys and values.
	 * @param extraHeaders Optional request headers merged into the request.
	 * @return The successful HTTP [Response].
	 */
	public suspend fun httpPost(url: HttpUrl, form: Map<String, String>, extraHeaders: Headers?): Response

	/**
	 * Performs a suspending HTTP `POST` request to [url] with a raw query/form string payload (`key=val&key2=val2`).
	 *
	 * @param url The target URL string.
	 * @param payload Raw string payload formatted with `&` delimiters.
	 * @return The successful HTTP [Response].
	 */
	public suspend fun httpPost(url: String, payload: String): Response = httpPost(url.toHttpUrl(), payload, null)

	/**
	 * Performs a suspending HTTP `POST` request to [url] with a raw query/form string payload (`key=val&key2=val2`).
	 *
	 * @param url The target [HttpUrl].
	 * @param payload Raw string payload formatted with `&` delimiters.
	 * @return The successful HTTP [Response].
	 */
	public suspend fun httpPost(url: HttpUrl, payload: String): Response = httpPost(url, payload, null)

	/**
	 * Performs a suspending HTTP `POST` request to [url] with a raw query/form string payload
	 * and optional [extraHeaders].
	 *
	 * @param url The target [HttpUrl].
	 * @param payload Raw string payload formatted with `&` delimiters.
	 * @param extraHeaders Optional request headers merged into the request.
	 * @return The successful HTTP [Response].
	 */
	public suspend fun httpPost(url: HttpUrl, payload: String, extraHeaders: Headers?): Response

	/**
	 * Performs a suspending HTTP `POST` request to [url] with a [JSONObject] payload (`application/json`).
	 *
	 * @param url The target URL string.
	 * @param body JSON object request body.
	 * @return The successful HTTP [Response].
	 */
	public suspend fun httpPost(url: String, body: JSONObject): Response = httpPost(url.toHttpUrl(), body, null)

	/**
	 * Performs a suspending HTTP `POST` request to [url] with a [JSONObject] payload (`application/json`).
	 *
	 * @param url The target [HttpUrl].
	 * @param body JSON object request body.
	 * @return The successful HTTP [Response].
	 */
	public suspend fun httpPost(url: HttpUrl, body: JSONObject): Response = httpPost(url, body, null)

	/**
	 * Performs a suspending HTTP `POST` request to [url] with a [JSONObject] payload (`application/json`)
	 * and optional [extraHeaders].
	 *
	 * @param url The target [HttpUrl].
	 * @param body JSON object request body.
	 * @param extraHeaders Optional request headers merged into the request.
	 * @return The successful HTTP [Response].
	 */
	public suspend fun httpPost(url: HttpUrl, body: JSONObject, extraHeaders: Headers?): Response

	/**
	 * Dispatches a GraphQL query to the specified [endpoint] and returns the parsed root [JSONObject].
	 *
	 * Automatically wraps [query] in root brackets if required, inspects the response for GraphQL errors,
	 * and throws [tsuki.exception.GraphQLException] if errors are present.
	 *
	 * @param endpoint The GraphQL endpoint URL string.
	 * @param query The GraphQL query document string.
	 * @return The parsed JSON response object.
	 * @throws tsuki.exception.GraphQLException if the response contains GraphQL error entries.
	 */
	public suspend fun graphQLQuery(endpoint: String, query: String): JSONObject
}

