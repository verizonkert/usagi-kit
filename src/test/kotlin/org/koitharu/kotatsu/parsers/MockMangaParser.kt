package tsuki

import okhttp3.Headers
import tsuki.config.ConfigKey
import tsuki.core.PagedMangaParser
import tsuki.model.*
import java.util.EnumSet

/**
 * In-memory mock implementation of [PagedMangaParser] providing reproducible test data
 * without making external network calls.
 */
class MockMangaParser(
	context: MangaLoaderContext,
	source: MangaSource = TestMangaSource.MOCK,
) : PagedMangaParser(context, source, pageSize = 10), MangaParserAuthProvider {

	override val configKeyDomain: ConfigKey.Domain = ConfigKey.Domain("example.com")

	override val availableSortOrders: Set<SortOrder> = EnumSet.of(SortOrder.RELEVANCE, SortOrder.POPULARITY, SortOrder.NEWEST)

	override val filterCapabilities: MangaListFilterCapabilities = MangaListFilterCapabilities(
		isSearchSupported = true,
		isMultipleTagsSupported = true,
	)

	override val authUrl: String = "https://example.com/login"

	override suspend fun isAuthorized(): Boolean = true

	override suspend fun getUsername(): String = "MockUser"

	override suspend fun getFilterOptions(): MangaListFilterOptions {
		return MangaListFilterOptions(
			availableTags = setOf(
				MangaTag(key = "action", title = "Action", source = source),
				MangaTag(key = "comedy", title = "Comedy", source = source),
			),
			availableStates = EnumSet.allOf(MangaState::class.java),
			availableContentRating = EnumSet.allOf(ContentRating::class.java),
			availableContentTypes = EnumSet.allOf(ContentType::class.java),
			availableDemographics = EnumSet.allOf(Demographic::class.java),
		)
	}

	override fun getRequestHeaders(): Headers = Headers.Builder().build()

	private val dummyCatalog: List<Manga> = (1..30).map { i ->
		Manga(
			id = i.toLong(),
			title = "Mock Manga $i",
			altTitles = emptySet(),
			url = "/manga/mock-$i",
			publicUrl = "https://example.com/manga/mock-$i",
			rating = 0.85f,
			contentRating = ContentRating.SAFE,
			coverUrl = "https://example.com/covers/mock-$i.jpg",
			tags = setOf(MangaTag(title = "Action", key = "action", source = source)),
			state = MangaState.ONGOING,
			authors = setOf("Author $i"),
			source = source,
		)
	}

	override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
		val startIndex = (page - 1) * pageSize
		if (startIndex !in dummyCatalog.indices) return emptyList()

		var results = dummyCatalog
		val matchTitle = filter.query
		if (!matchTitle.isNullOrBlank()) {
			results = results.filter { it.title.contains(matchTitle, ignoreCase = true) }
		}

		val endIndex = minOf(startIndex + pageSize, results.size)
		return if (startIndex < results.size) results.subList(startIndex, endIndex) else emptyList()
	}

	override suspend fun getDetails(manga: Manga): Manga {
		val chapters = (1..5).map { chapNum ->
			MangaChapter(
				id = manga.id * 100 + chapNum,
				title = "Chapter $chapNum",
				number = chapNum.toFloat(),
				volume = 1,
				url = "${manga.url}/chapter-$chapNum",
				scanlator = "ScanGroup",
				uploadDate = System.currentTimeMillis(),
				branch = "default",
				source = source,
			)
		}
		return manga.copy(
			description = "Test description for ${manga.title}",
			chapters = chapters,
		)
	}

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		return (1..3).map { pageNum ->
			MangaPage(
				id = chapter.id * 10 + pageNum,
				url = "https://example.com/pages/${chapter.id}/$pageNum.jpg",
				preview = null,
				source = source,
			)
		}
	}
}
