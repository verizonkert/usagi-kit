package tsuki.util

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import tsuki.MangaLoaderContextMock
import tsuki.MockMangaParser
import tsuki.TestMangaSource
import kotlin.time.Duration.Companion.minutes

internal class LinkResolverTest {

	private val context = MangaLoaderContextMock

	@Test
	fun resolveLinkWithMockParser() = runTest(timeout = 2.minutes) {
		val parser = MockMangaParser(context, TestMangaSource.MOCK)
		val resolver = context.newLinkResolver("https://example.com/manga/mock-1")
		val manga = parser.resolveLink("https://example.com/manga/mock-1")
		// Resolving link via parser directly
		assertEquals(TestMangaSource.MOCK, parser.source)
	}
}
