package tsuki

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.params.ParameterizedTest
import tsuki.core.PagedMangaParser
import tsuki.core.SinglePageMangaParser
import tsuki.model.*
import tsuki.model.search.MangaSearchQuery
import tsuki.model.search.QueryCriteria
import tsuki.model.search.QueryCriteria.Include
import tsuki.model.search.SearchableField.*
import tsuki.util.medianOrNull
import org.koitharu.kotatsu.test_util.*
import kotlin.time.Duration.Companion.minutes

//@ExtendWith(AuthCheckExtension::class)
internal class MangaParserTest {

	private val context = MangaLoaderContextMock
	private val timeout = 2.minutes

	@ParameterizedTest(name = "{index}|list|{0}")
	@MangaSources
	fun list(source: MangaSource) = runTest(timeout = timeout) {
		val parser = context.newParserInstance(source)
		val list = parser.getList(MangaSearchQuery.Builder().build())
		checkMangaList(list, "list")
		assert(list.all { it.source == source })
	}

	@ParameterizedTest(name = "{index}|pagination|{0}")
	@MangaSources
	fun pagination(source: MangaSource) = runTest(timeout = timeout) {
		val parser = context.newParserInstance(source)
		if (parser is SinglePageMangaParser) {
			return@runTest
		}
		val page1 = parser.getList(MangaSearchQuery.EMPTY)
		val page2 =
			parser.getList(MangaSearchQuery.Builder().offset(page1.size).build())
		if (parser is PagedMangaParser) {
			assert(parser.pageSize >= page1.size) {
				"Page size is ${page1.size} but ${parser.pageSize} expected"
			}
		}
		assert(page1.isNotEmpty()) { "Page 1 is empty" }
		assert(page2.isNotEmpty()) { "Page 2 is empty" }
		assert(page1 != page2) { "Pages are equal" }
		val intersection = page1.intersect(page2.toSet())
		assert(intersection.isEmpty()) {
			"Pages are intersected by " + intersection.size
		}
	}

	@ParameterizedTest(name = "{index}|search|{0}")
	@MangaSources
	fun searchByTitleName(source: MangaSource) = runTest(timeout = timeout) {
		val parser = context.newParserInstance(source)
		val subject = parser.getList(MangaSearchQuery.EMPTY).minByOrNull {
			it.title.length
		} ?: error("No manga found")

		val query = subject.title
		check(query.isNotBlank()) { "Manga title '$query' is blank" }
		val list = parser.getList(
			MangaSearchQuery.Builder()
				.order(SortOrder.RELEVANCE)
				.criterion(QueryCriteria.Match(TITLE_NAME, query))
				.build(),
		)
		assert(list.isNotEmpty()) { "Empty search results by \"$query\"" }
		assert(list.singleOrNull { it.url == subject.url && it.id == subject.id } != null) {
			"Single subject '${subject.title} (${subject.publicUrl})' not found in search results"
		}
		checkMangaList(list, "search('$query')")
		assert(list.all { it.source == source })
	}

	@ParameterizedTest(name = "{index}|details|{0}")
	@MangaSources
	fun details(source: MangaSource) = runTest(timeout = timeout) {
		val parser = context.newParserInstance(source)
		val list = parser.getList(MangaSearchQuery.EMPTY)

		val manga = list.random()
		parser.getDetails(manga).apply {
			assert(!chapters.isNullOrEmpty()) { "Chapters are null or empty" }
			assert(publicUrl.isUrlAbsolute()) { "Manga public url is not absolute: '$publicUrl'" }
			assert(description != null) { "Detailed description is null: '$publicUrl'" }
			assert(title.startsWith(manga.title)) {
				"Titles are mismatch: '$title' and '${manga.title}' for $publicUrl"
			}
			assert(this.source == source)
			val c = checkNotNull(chapters)
			assert(c.isDistinctBy { it.id }) {
				"Chapters are not distinct by id: ${c.maxDuplicates { it.id }} for $publicUrl"
			}
			assert(c.isDistinctByNotNull { it.key() }) {
				val dup = c.mapNotNull { it.key() }.maxDuplicates { it }
				"Chapters are not distinct by branch/volume/number: $dup for $publicUrl"
			}
			assert(c.all { it.source == source })
		}
	}

	@ParameterizedTest(name = "{index}|pages|{0}")
	@MangaSources
	fun pages(source: MangaSource) = runTest(timeout = timeout) {
		val parser = context.newParserInstance(source)
		val list = parser.getList(MangaSearchQuery.EMPTY)
		val manga = list.first()
		val chapter = parser.getDetails(manga).chapters?.firstOrNull() ?: error("Chapter is null at ${manga.publicUrl}")
		val pages = parser.getPages(chapter)

		assert(pages.isNotEmpty())
		assert(pages.isDistinctBy { it.id })
		assert(pages.all { it.source == source })

		arrayOf(
			pages.first(),
			pages.medianOrNull() ?: error("No page"),
		).forEach { page ->
			val pageUrl = parser.getPageUrl(page)
			assert(pageUrl.isNotEmpty())
			assert(pageUrl.isUrlAbsolute())
		}
	}

	@ParameterizedTest(name = "{index}|authorization|{0}")
	@MangaSources
	fun authorization(source: MangaSource) = runTest(timeout = timeout) {
		val parser = context.newParserInstance(source)
		if (parser is MangaParserAuthProvider) {
			val username = parser.getUsername()
			assert(!username.isNullOrBlank()) { "Username is blank or null" }
			println("Signed in to ${source.name} as $username")
		}
	}

	private fun checkMangaList(list: List<Manga>, cause: String) {
		assert(list.isNotEmpty()) { "Manga list for '$cause' is empty" }
		assert(list.isDistinctBy { it.id }) { "Manga list for '$cause' contains duplicated ids" }
		for (item in list) {
			assert(item.url.isNotEmpty()) { "Url is empty" }
			assert(!item.url.isUrlAbsolute()) { "Url looks like absolute: ${item.url}" }
			item.coverUrl?.let {
				assert(it.isUrlAbsolute()) { "Cover url is not absolute: ${item.coverUrl}" }
			}
			assert(item.title.isNotEmpty()) { "Title for ${item.publicUrl} is empty" }
			assert(item.publicUrl.isUrlAbsolute())
		}
	}

	private fun MangaChapter.key(): Any? = when {
		number > 0f && volume > 0 -> Triple(branch, volume, number)
		number > 0f -> Pair(branch, number)
		else -> null
	}
}
