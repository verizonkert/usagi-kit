package org.draken.usagikit.context

import android.graphics.BitmapFactory
import android.graphics.Canvas
import androidx.core.graphics.createBitmap
import tsuki.bitmap.Bitmap
import tsuki.bitmap.Rect
import java.io.OutputStream
import android.graphics.Bitmap as AndroidBitmap
import android.graphics.Rect as AndroidRect

class UsagiBitmapWrapper private constructor(
    val androidBitmap: AndroidBitmap,
) : Bitmap, AutoCloseable {

    private val canvas by lazy { Canvas(androidBitmap) }

    override val height: Int
        get() = androidBitmap.height

    override val width: Int
        get() = androidBitmap.width

    override fun drawBitmap(
        sourceBitmap: Bitmap,
        src: Rect,
        dst: Rect,
    ) {
        val srcBitmap = (sourceBitmap as UsagiBitmapWrapper).androidBitmap
        canvas.drawBitmap(srcBitmap, src.toAndroidRect(), dst.toAndroidRect(), null)
    }

    override fun close() {
        androidBitmap.recycle()
    }

    fun compressTo(output: OutputStream) {
        androidBitmap.compress(AndroidBitmap.CompressFormat.PNG, 100, output)
    }

    companion object {
        fun create(width: Int, height: Int) = UsagiBitmapWrapper(
            createBitmap(width, height, AndroidBitmap.Config.ARGB_8888)
        )

        fun create(bitmap: AndroidBitmap) = UsagiBitmapWrapper(
            if (bitmap.isMutable) bitmap else bitmap.copy(AndroidBitmap.Config.ARGB_8888, true)
        )

        fun decode(bytes: ByteArray): UsagiBitmapWrapper {
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: createBitmap(1, 1, AndroidBitmap.Config.ARGB_8888)
            return create(bitmap)
        }

        private fun Rect.toAndroidRect() = AndroidRect(left, top, right, bottom)
    }
}
