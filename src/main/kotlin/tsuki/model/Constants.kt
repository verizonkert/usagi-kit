@file:JvmName("Constants")

package tsuki.model

/**
 * Sentinel value representing an unknown, unrated, or unassigned manga rating score.
 */
public const val RATING_UNKNOWN: Float = -1f

/**
 * Sentinel value representing an unknown or unspecified release/publication year.
 */
public const val YEAR_UNKNOWN: Int = 0

/**
 * The earliest reasonable publication year supported by the manga platform catalog (1900).
 */
public const val YEAR_MIN: Int = 1900

/**
 * The maximum forward-looking publication year supported by the manga platform catalog (2099).
 */
public const val YEAR_MAX: Int = 2099

