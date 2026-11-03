package tsuki

import okhttp3.Interceptor
import okhttp3.Response
import tsuki.model.MangaSource
import tsuki.network.CommonHeaders

private const val HEADER_REFERER = CommonHeaders.REFERER

internal class CommonHeadersInterceptor : Interceptor {

	override fun intercept(chain: Interceptor.Chain): Response {
		val request = chain.request()
		val source = request.tag(MangaSource::class.java)
		val parser = if (source != null) {
			MangaLoaderContextMock.newParserInstance(source)
		} else {
			null
		}
		val headersBuilder = request.headers.newBuilder()
		if (headersBuilder[HEADER_REFERER] == null && parser != null) {
			headersBuilder[HEADER_REFERER] = "https://${parser.domain}/"
		}
		val newRequest = request.newBuilder().headers(headersBuilder.build()).build()
		return if (parser is Interceptor) {
			parser.intercept(ProxyChain(chain, newRequest))
		} else {
			chain.proceed(newRequest)
		}
	}

	private class ProxyChain(
		private val origin: Interceptor.Chain,
		private val request: okhttp3.Request,
	) : Interceptor.Chain by origin {

		override fun request(): okhttp3.Request = request
	}
}
