package tsuki.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import tsuki.MangaParser
import tsuki.model.Manga
import tsuki.model.MangaListFilter
import tsuki.model.SortOrder

/**
 * Searches for related or duplicate manga across one or multiple [MangaParser] instances.
 *
 * Deconstructs the seed manga's title and alternative titles into distinct search keywords,
 * queries the parser(s) concurrently, and aggregates matching titles that share significant keyword overlap.
 *
 * @param parsers The collection of manga parsers to query for related titles.
 */
public class RelatedMangaFinder(
    private val parsers: Collection<MangaParser>,
) {

    /**
     * Executes the related manga search using the metadata of [seed].
     *
     * @param seed Source manga whose title and alternate titles serve as search seeds.
     * @return List of discovered related [Manga] instances.
     */
    public suspend operator fun invoke(seed: Manga): List<Manga> = withContext(Dispatchers.Default) {
        coroutineScope {
            parsers.singleOrNull()?.let { parser ->
                findRelatedImpl(this, parser, seed)
            } ?: parsers.map { parser ->
                async {
                    findRelatedImpl(this, parser, seed)
                }
            }.awaitAll().flatten()
        }
    }

    /**
     * Executes keyword search queries on an individual [parser] and filters results matching the seed.
     */
    private suspend fun findRelatedImpl(scope: CoroutineScope, parser: MangaParser, seed: Manga): List<Manga> {
        val words = HashSet<String>()
        words += seed.title.splitByWhitespace()
        seed.altTitles.forEach {
            words += it.splitByWhitespace()
        }
        if (words.isEmpty()) {
            return emptyList()
        }
        val results = words.map { keyword ->
            scope.async {
                val result = runCatching {
                    parser.getList(
                        0,
                        if (SortOrder.RELEVANCE in parser.availableSortOrders) {
                            SortOrder.RELEVANCE
                        } else {
                            parser.availableSortOrders.first()
                        },
                        MangaListFilter(
                            query = keyword,
                        ),
                    )
                }.getOrElse { emptyList() }
                result.filter { it.id != seed.id && it.containKeyword(keyword) }
            }
        }.awaitAll()
        return results.minByOrNull { if (it.isEmpty()) Int.MAX_VALUE else it.size } ?: emptyList()
    }

    /**
     * Checks if this manga's primary title or alternative titles contain the search [keyword].
     */
    private fun Manga.containKeyword(keyword: String): Boolean {
        return title.contains(keyword, ignoreCase = true)
            || altTitles.any { it.contains(keyword, ignoreCase = true) }
    }
}
