package tsuki

import tsuki.model.ContentType
import tsuki.model.MangaSource

/**
 * Standard test implementation of [MangaSource] used across unit and integration tests.
 */
enum class TestMangaSource(
	override val title: String,
	override val locale: String = "en",
	override val contentType: ContentType = ContentType.MANGA,
	override val isBroken: Boolean = false,
) : MangaSource {
	MOCK("Mock Source"),
	MANGADEX("MangaDex"),
	BATOTO("Bato.to"),
	TEST_SOURCE("Test Manga Provider"),
}
