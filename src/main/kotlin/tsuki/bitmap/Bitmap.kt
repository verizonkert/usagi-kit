package tsuki.bitmap

/**
 * Platform-independent abstraction representing a 2D raster image / bitmap.
 *
 * Used primarily by image descramblers to decode, slice, and reconstruct scrambled
 * manga pages (such as puzzle or tiled canvas images) into their original layout.
 */
public interface Bitmap {

	/**
	 * Width of this bitmap in pixels.
	 */
	public val width: Int

	/**
	 * Height of this bitmap in pixels.
	 */
	public val height: Int

	/**
	 * Draws a rectangular region from [sourceBitmap] onto this bitmap canvas.
	 *
	 * Copies pixels bounded by [src] from [sourceBitmap] and renders them into the region bounded by [dst].
	 * If [src] and [dst] dimensions differ, the drawn image content is scaled to fit [dst].
	 *
	 * @param sourceBitmap The source image from which pixel data is read.
	 * @param src The source bounding rectangle specifying the subsection of [sourceBitmap] to copy.
	 * @param dst The destination bounding rectangle specifying where on this bitmap the pixels are drawn.
	 */
	public fun drawBitmap(sourceBitmap: Bitmap, src: Rect, dst: Rect)
}

