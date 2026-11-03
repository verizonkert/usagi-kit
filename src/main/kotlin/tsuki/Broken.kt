package tsuki

/**
 * Marks a [MangaParser] implementation as currently non-functional (e.g. website shut down,
 * under maintenance, or permanently altered by Cloudflare/DDoS guards) without deleting the class.
 *
 * Applying `@Broken` allows the catalog and bytecode registries to retain the source definition
 * while preventing users from attempting to fetch or search broken endpoints until a fix is deployed.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
public annotation class Broken(

	/**
	 * Explanatory reason or issue tracker URL describing why this source is broken.
	 */
	val message: String = "",
)
