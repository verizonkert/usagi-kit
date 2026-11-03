package tsuki.util

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CompletionHandler
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import okhttp3.internal.closeQuietly
import java.io.IOException
import kotlin.coroutines.resumeWithException

/**
 * Bridges OkHttp's asynchronous [Callback] with Kotlin Coroutines' [CancellableContinuation].
 *
 * Implements [Callback] to receive network responses or failures and resume the coroutine,
 * and implements [CompletionHandler] to cancel the underlying OkHttp [Call] if the coroutine is cancelled.
 *
 * @property call The active OkHttp HTTP call being monitored.
 * @property continuation The coroutine continuation awaiting the [Response].
 */
public class ContinuationCallCallback(
    private val call: Call,
    private val continuation: CancellableContinuation<Response>,
) : Callback, CompletionHandler {

    /**
     * Invoked when an HTTP response is returned by the HTTP server.
     *
     * Resumes the continuation with the received [Response]. If the coroutine is no longer active,
     * the response body is safely closed to prevent resource leaks.
     */
    override fun onResponse(call: Call, response: Response) {
        if (!continuation.isActive) {
            response.closeQuietly()
            return
        }
        continuation.resume(response) { _, value, _ ->
            value.closeQuietly()
        }
    }

    /**
     * Invoked when the request could not be executed due to cancellation, a connectivity problem, or a timeout.
     */
    override fun onFailure(call: Call, e: IOException) {
        if (continuation.isActive) {
            continuation.resumeWithException(e)
        }
    }

    /**
     * Invoked when the awaiting coroutine is cancelled, ensuring the underlying network call is aborted.
     */
    override fun invoke(cause: Throwable?) {
        runCatching {
            call.cancel()
        }.onFailure { e ->
            cause?.addSuppressed(e)
        }
    }
}
