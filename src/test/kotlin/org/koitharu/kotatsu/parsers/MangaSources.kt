package tsuki

import org.junit.jupiter.params.provider.EnumSource

/**
 * Parameterized source provider for parser tests using [TestMangaSource].
 */
@EnumSource(TestMangaSource::class, names = ["MOCK"], mode = EnumSource.Mode.INCLUDE)
internal annotation class MangaSources
