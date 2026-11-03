@file:JvmName("OkHttpUtils")

package tsuki.util

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.Response
import okhttp3.ResponseBody
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Asynchronously executes this OkHttp [Call] and awaits its [Response] within a cancellable coroutine.
 *
 * If the coroutine is cancelled while the network request is in flight, the call is cleanly aborted.
 *
 * @return The received HTTP [Response].
 * @throws java.io.IOException if the network request fails.
 */
public suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
    val callback = ContinuationCallCallback(this, continuation)
    continuation.invokeOnCancellation(callback)
    enqueue(callback)
}

/**
 * Extracts and normalizes the MIME type string from the `Content-Type` header (e.g. `"text/html"`, `"image/jpeg"`).
 */
public val Response.mimeType: String?
    get() = header("content-type")?.substringBefore(';')?.trim()?.nullIfEmpty()?.lowercase()

/**
 * Returns `true` if this URL uses either the `http` or `https` protocol scheme.
 */
public val HttpUrl.isHttpOrHttps: Boolean
    get() = scheme.equals("https", ignoreCase = true) || scheme.equals("http", ignoreCase = true)

/**
 * Merges headers from [other] into this builder.
 *
 * @param other Source headers to copy from.
 * @param replaceExisting If `true`, overwrites existing headers with the same name.
 * @return This builder instance for chaining.
 */
public fun Headers.Builder.mergeWith(other: Headers, replaceExisting: Boolean): Headers.Builder {
    for (name in other.names()) {
        val value = other[name] ?: continue
        if (replaceExisting || this[name] == null) {
            this[name] = value
        }
    }
    return this
}

/**
 * Clones this [Response] by peeking its body stream up to 10MB without consuming the original response body.
 */
public fun Response.copy(): Response = newBuilder()
    .body(peekBody(DEFAULT_PEEK_BODY_LIMIT))
    .build()

private const val DEFAULT_PEEK_BODY_LIMIT = 10L * 1024 * 1024 // 10MB safe ceiling

/**
 * Sets a header value on this builder, or removes it if [value] is `null`.
 */
public fun Response.Builder.setHeader(name: String, value: String?): Response.Builder = if (value == null) {
    removeHeader(name)
} else {
    header(name, value)
}

/**
 * Transforms the [ResponseBody] of this response using [mapper], ensuring the previous body is cleanly closed.
 */
public inline fun Response.map(mapper: (ResponseBody) -> ResponseBody): Response {
    contract {
        callsInPlace(mapper, InvocationKind.AT_MOST_ONCE)
    }
    return body.use { responseBody ->
        newBuilder()
            .body(mapper(responseBody))
            .build()
    }
}

/**
 * Returns the numeric content length in bytes declared by the `Content-Length` header, or `-1L` if absent or invalid.
 */
public fun Response.headersContentLength(): Long = headersContentLength

/**
 * The numeric content length in bytes declared by the `Content-Length` header, or `-1L` if absent or invalid.
 */
public val Response.headersContentLength: Long
	get() = header("Content-Length")?.toLongOrNull() ?: -1L
