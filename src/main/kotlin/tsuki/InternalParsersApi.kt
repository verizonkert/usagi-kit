package tsuki

/**
 * Opt-in marker annotation distinguishing advanced internal engine routines and parser plumbing.
 *
 * Declarations marked with `@InternalParsersApi` are intended for internal engine use,
 * base abstract classes, or specialized parser extension utilities. They may have evolving contracts
 * or rely on low-level runtime internals.
 *
 * To consume an API annotated with `@InternalParsersApi`:
 * - Annotate the calling function or file with `@OptIn(InternalParsersApi::class)`
 * - Or pass `-opt-in=tsuki.InternalParsersApi` as a Kotlin compiler argument in `build.gradle.kts`.
 */
@Retention(AnnotationRetention.BINARY)
@SinceKotlin("1.3")
@RequiresOptIn(
	level = RequiresOptIn.Level.WARNING,
	message = "This is an internal Tsuki engine API. It should only be used by parser implementations and base classes.",
)
@MustBeDocumented
public annotation class InternalParsersApi
