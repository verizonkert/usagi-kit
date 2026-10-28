package org.draken.usagikit.context

import android.content.Context
import android.util.Base64
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.draken.usagikit.UsagiKit
import tsuki.MangaLoaderContext
import tsuki.MangaParser
import tsuki.bitmap.Bitmap
import tsuki.config.ConfigKey
import tsuki.config.MangaSourceConfig
import tsuki.model.MangaSource
import tsuki.network.UserAgents
import java.io.ByteArrayOutputStream
import java.util.Locale

/**
 * Default implementation of [MangaLoaderContext] for standalone UsagiKit usage.
 */
class DefaultUsagiLoaderContext(
    val context: Context,
    override val httpClient: OkHttpClient = UsagiKit.okHttpClient,
    override val cookieJar: UsagiCookieJar = UsagiCookieJar(),
    private val userAgent: String = UserAgents.CHROME_MOBILE,
) : MangaLoaderContext() {

    override fun getDefaultUserAgent(): String = userAgent

    override fun getParserSources(): List<MangaSource> = UsagiKit.getAllSources()

    override fun newParserInstance(source: MangaSource): MangaParser = UsagiKit.getParser(source)

    override fun getConfig(source: MangaSource): MangaSourceConfig {
        return SharedPreferencesSourceConfig(context, source)
    }

    override fun encodeBase64(data: ByteArray): String = Base64.encodeToString(data, Base64.NO_WRAP)

    override fun decodeBase64(data: String): ByteArray = Base64.decode(data, Base64.DEFAULT)

    override fun getPreferredLocales(): List<Locale> = listOf(Locale.getDefault(), Locale.ENGLISH)

    override fun requestBrowserAction(parser: MangaParser, url: String): Nothing {
        throw UnsupportedOperationException("Interactive browser action required for URL: $url")
    }

    override fun redrawImageResponse(response: Response, redraw: (image: Bitmap) -> Bitmap): Response {
        val body = response.body ?: return response
        val bytes = body.bytes()
        val bitmap = UsagiBitmapWrapper.decode(bytes)
        val transformed = redraw(bitmap) as UsagiBitmapWrapper
        val output = ByteArrayOutputStream()
        transformed.compressTo(output)
        val newBody = output.toByteArray().toResponseBody("image/png".toMediaType())
        return response.newBuilder().body(newBody).build()
    }

    override fun createBitmap(width: Int, height: Int): Bitmap {
        return UsagiBitmapWrapper.create(width, height)
    }

    @Deprecated("Provide a base url")
    override suspend fun evaluateJs(script: String): String? = evaluateJs("", script)

    override suspend fun evaluateJs(baseUrl: String, script: String): String? {
        return null
    }

    private class SharedPreferencesSourceConfig(
        context: Context,
        source: MangaSource,
    ) : MangaSourceConfig {
        private val prefs = context.getSharedPreferences("usagi_source_${source.name}", Context.MODE_PRIVATE)

        @Suppress("UNCHECKED_CAST")
        override fun <T> get(key: ConfigKey<T>): T =
            when (key) {
                is ConfigKey.UserAgent -> {
                    prefs.getString(key.key, key.defaultValue) ?: key.defaultValue
                }
                is ConfigKey.Domain -> {
                    prefs.getString(key.key, key.defaultValue) ?: key.defaultValue
                }
                is ConfigKey.ShowSuspiciousContent -> {
                    prefs.getBoolean(key.key, key.defaultValue)
                }
                is ConfigKey.SplitByTranslations -> {
                    prefs.getBoolean(key.key, key.defaultValue)
                }
                is ConfigKey.PreferredImageServer -> {
                    prefs.getString(key.key, key.defaultValue)
                }
            } as T
    }
}
