package app.usagi.tester

import android.graphics.BitmapFactory
import okhttp3.CookieJar
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import tsuki.MangaParser
import tsuki.bitmap.Bitmap
import tsuki.bitmap.Rect
import tsuki.config.ConfigKey
import tsuki.config.MangaSourceConfig
import tsuki.model.MangaSource
import java.io.ByteArrayOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class TestMangaLoaderContext(
    override val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build(),
    override val cookieJar: CookieJar = CookieJar.NO_COOKIES,
) : MangaLoaderContext() {

    private val configs = ConcurrentHashMap<String, MangaSourceConfig>()

    override fun newParserInstance(source: MangaSource): MangaParser {
        throw UnsupportedOperationException("Source parser instantiation handled dynamically")
    }

    override fun getParserSources(): List<MangaSource> = emptyList()

    override suspend fun evaluateJs(script: String): String? = null

    override suspend fun evaluateJs(baseUrl: String, script: String): String? = null

    override fun getConfig(source: MangaSource): MangaSourceConfig {
        return configs.computeIfAbsent(source.name) {
            TestMangaSourceConfig()
        }
    }

    override fun getDefaultUserAgent(): String {
        return "Usagi/1.0 (Android 14; Mobile; +https://github.com/UsagiApp)"
    }

    override fun redrawImageResponse(
        response: Response,
        redraw: (image: Bitmap) -> Bitmap,
    ): Response {
        val body = response.body ?: return response
        val bytes = body.bytes()
        val androidBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return response
        val wrappedBitmap = AndroidBitmapWrapper(androidBitmap)
        val redrawn = redraw(wrappedBitmap) as AndroidBitmapWrapper

        val out = ByteArrayOutputStream()
        redrawn.innerBitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
        val newBytes = out.toByteArray()

        return response.newBuilder()
            .body(newBytes.toResponseBody(body.contentType()))
            .build()
    }

    override fun createBitmap(width: Int, height: Int): Bitmap {
        val bmp = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
        return AndroidBitmapWrapper(bmp)
    }

    private class AndroidBitmapWrapper(val innerBitmap: android.graphics.Bitmap) : Bitmap {
        override val width: Int get() = innerBitmap.width
        override val height: Int get() = innerBitmap.height

        override fun drawBitmap(sourceBitmap: Bitmap, src: Rect, dst: Rect) {
            val canvas = android.graphics.Canvas(innerBitmap)
            val srcRect = android.graphics.Rect(src.left, src.top, src.right, src.bottom)
            val dstRect = android.graphics.Rect(dst.left, dst.top, dst.right, dst.bottom)
            val srcBmp = (sourceBitmap as AndroidBitmapWrapper).innerBitmap
            canvas.drawBitmap(srcBmp, srcRect, dstRect, null)
        }
    }

    private class TestMangaSourceConfig : MangaSourceConfig {
        private val map = ConcurrentHashMap<String, Any>()

        @Suppress("UNCHECKED_CAST")
        override fun <T> get(key: ConfigKey<T>): T {
            return (map[key.key] as? T) ?: key.defaultValue
        }
    }
}
