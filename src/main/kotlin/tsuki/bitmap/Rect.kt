package tsuki.bitmap

/**
 * Immutable rectangular bounding box defined by integer pixel coordinates.
 *
 * Coordinates are 0-based with the origin `(0, 0)` at the top-left corner.
 * The rectangle spans horizontally from [left] up to [right], and vertically from [top] down to [bottom].
 *
 * @property left The horizontal X coordinate of the left edge (inclusive). Defaults to 0.
 * @property top The vertical Y coordinate of the top edge (inclusive). Defaults to 0.
 * @property right The horizontal X coordinate of the right edge (exclusive). Defaults to 0.
 * @property bottom The vertical Y coordinate of the bottom edge (exclusive). Defaults to 0.
 */
public data class Rect(
	val left: Int = 0,
	val top: Int = 0,
	val right: Int = 0,
	val bottom: Int = 0,
) {

	/**
	 * Width of this rectangle in pixels (`right - left`).
	 */
	val width: Int
		get() = right - left

	/**
	 * Height of this rectangle in pixels (`bottom - top`).
	 */
	val height: Int
		get() = bottom - top
}

