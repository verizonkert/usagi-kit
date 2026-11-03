package tsuki

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import tsuki.bitmap.Bitmap
import tsuki.config.MangaSourceConfig
import tsuki.model.MangaSource
import tsuki.network.UserAgents
import tsuki.util.await
import tsuki.util.requireBody
import org.koitharu.kotatsu.test_util.BitmapTestImpl
import java.awt.image.BufferedImage
import java.io.InputStream
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager

internal object MangaLoaderContextMock : MangaLoaderContext() {

	override val cookieJar = InMemoryCookieJar()

	override val httpClient: OkHttpClient = OkHttpClient.Builder()
		.cookieJar(cookieJar)
		.permissiveSSL()
		.addInterceptor(CommonHeadersInterceptor())
		.addInterceptor(CloudFlareInterceptor())
		.connectTimeout(20, TimeUnit.SECONDS)
		.readTimeout(60, TimeUnit.SECONDS)
		.writeTimeout(20, TimeUnit.SECONDS)
		.build()

	init {
		loadTestCookies()
	}

	override fun newParserInstance(source: MangaSource): MangaParser {
		return MockMangaParser(this, source)
	}

	override fun getParserSources(): List<MangaSource> {
		return TestMangaSource.entries.toList()
	}

	@Deprecated("Provide a base url")
	override suspend fun evaluateJs(script: String): String? = evaluateJs("", script)

	override suspend fun evaluateJs(baseUrl: String, script: String): String? {
		return script
	}

	override fun getConfig(source: MangaSource): MangaSourceConfig {
		return SourceConfigMock()
	}

	override fun getDefaultUserAgent(): String = UserAgents.FIREFOX_MOBILE

	override fun redrawImageResponse(response: Response, redraw: (Bitmap) -> Bitmap): Response {
		val srcImage = response.requireBody().byteStream().use(ImageIO::read)
		checkNotNull(srcImage) { "Cannot decode image" }
		val resImage = (redraw(BitmapTestImpl(srcImage)) as BitmapTestImpl)
		return response.newBuilder()
			.body(resImage.compress("png").toResponseBody("image/png".toMediaTypeOrNull()))
			.build()
	}

	override fun createBitmap(width: Int, height: Int): Bitmap {
		return BitmapTestImpl(BufferedImage(width, height, BufferedImage.TYPE_INT_RGB))
	}

	suspend fun doRequest(url: String, source: MangaSource?): Response {
		val request = Request.Builder()
			.get()
			.url(url)
		if (source != null) {
			request.tag(MangaSource::class.java, source)
		}
		return httpClient.newCall(request.build()).await()
	}

	private fun loadTestCookies() {
		javaClass.getResourceAsStream("/cookies.txt")?.use {
			cookieJar.loadFromStream(it)
		}
	}

	private fun OkHttpClient.Builder.permissiveSSL() = also { builder ->
		runCatching {
			val trustAllCerts = object : X509TrustManager {
				override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = Unit
				override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) = Unit
				override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
			}
			val sslContext = SSLContext.getInstance("SSL")
			sslContext.init(null, arrayOf(trustAllCerts), SecureRandom())
			val sslSocketFactory: SSLSocketFactory = sslContext.socketFactory
			builder.sslSocketFactory(sslSocketFactory, trustAllCerts)
			builder.hostnameVerifier { _, _ -> true }
		}.onFailure {
			it.printStackTrace()
		}
	}
}
