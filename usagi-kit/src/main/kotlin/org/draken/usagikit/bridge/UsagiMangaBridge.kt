package org.draken.usagikit.bridge

import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import tsuki.MangaLoaderContext
import tsuki.MangaParser
import tsuki.model.Manga
import tsuki.model.MangaChapter
import tsuki.model.MangaListFilter
import tsuki.model.MangaListFilterCapabilities
import tsuki.model.MangaListFilterOptions
import tsuki.model.MangaPage
import tsuki.model.MangaSource
import tsuki.model.SortOrder
import java.io.IOException

/**
 * Interface that bridges Usagi manga sources to uniform manga query, details, and page methods.
 */
interface UsagiMangaBridge {
    val source: MangaSource
    val availableSortOrders: Set<SortOrder>
    val filterCapabilities: MangaListFilterCapabilities

    suspend fun getList(page: Int = 1, sortOrder: SortOrder? = null, filter: MangaListFilter? = null): List<Manga>
    suspend fun getPopularManga(page: Int = 1): List<Manga>
    suspend fun getLatestManga(page: Int = 1): List<Manga>
    suspend fun searchManga(page: Int = 1, query: String = "", filter: MangaListFilter? = null): List<Manga>
    suspend fun getDetails(manga: Manga): Manga
    suspend fun getPages(chapter: MangaChapter): List<MangaPage>
    suspend fun getPageUrl(page: MangaPage): String
    suspend fun getFilterOptions(): MangaListFilterOptions
    suspend fun getPageRequest(page: MangaPage): Request
    suspend fun getPageResponse(page: MangaPage, client: OkHttpClient? = null): Response
    fun getHeaders(): Headers?
}

/**
 * Default implementation of [UsagiMangaBridge] wrapping a [MangaParser].
 */
open class DefaultUsagiMangaBridge(
    override val source: MangaSource,
    val parser: MangaParser,
    val loaderContext: MangaLoaderContext,
) : UsagiMangaBridge {

    override val availableSortOrders: Set<SortOrder>
        get() = parser.availableSortOrders

    override val filterCapabilities: MangaListFilterCapabilities
        get() = parser.filterCapabilities

    override suspend fun getList(page: Int, sortOrder: SortOrder?, filter: MangaListFilter?): List<Manga> {
        val offset = (page - 1).coerceAtLeast(0) * 20
        val sort = sortOrder ?: availableSortOrders.firstOrNull() ?: SortOrder.POPULARITY
        return parser.getList(offset, sort, filter ?: MangaListFilter.EMPTY)
            .map { it.copy(source = source) }
    }

    override suspend fun getPopularManga(page: Int): List<Manga> {
        return getList(page, SortOrder.POPULARITY, MangaListFilter.EMPTY)
    }

    override suspend fun getLatestManga(page: Int): List<Manga> {
        val offset = (page - 1).coerceAtLeast(0) * 20
        val sort = if (availableSortOrders.contains(SortOrder.UPDATED)) {
            SortOrder.UPDATED
        } else if (availableSortOrders.contains(SortOrder.NEWEST)) {
            SortOrder.NEWEST
        } else {
            availableSortOrders.firstOrNull() ?: SortOrder.POPULARITY
        }
        return parser.getList(offset, sort, MangaListFilter.EMPTY)
            .map { it.copy(source = source) }
    }

    override suspend fun searchManga(page: Int, query: String, filter: MangaListFilter?): List<Manga> {
        val offset = (page - 1).coerceAtLeast(0) * 20
        val appliedFilter = filter ?: MangaListFilter(query = query)
        val sort = if (query.isNotBlank() && availableSortOrders.contains(SortOrder.RELEVANCE)) {
            SortOrder.RELEVANCE
        } else {
            availableSortOrders.firstOrNull() ?: SortOrder.POPULARITY
        }
        return parser.getList(offset, sort, appliedFilter)
            .map { it.copy(source = source) }
    }

    override suspend fun getDetails(manga: Manga): Manga {
        val details = parser.getDetails(manga)
        return details.copy(
            source = source,
            chapters = details.chapters?.map { it.copy(source = source) },
        )
    }

    override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
        return parser.getPages(chapter).map {
            it.copy(source = source)
        }
    }

    override suspend fun getPageUrl(page: MangaPage): String {
        val resolved = parser.getPageUrl(page)
        return resolved.ifEmpty { page.url }
    }

    override suspend fun getFilterOptions(): MangaListFilterOptions {
        return parser.getFilterOptions()
    }

    override suspend fun getPageRequest(page: MangaPage): Request {
        val url = getPageUrl(page)
        val headers = getHeaders() ?: Headers.headersOf(
            "Accept", "image/webp,image/png;q=0.9,image/jpeg,*/*;q=0.8",
            "Cache-Control", "no-cache"
        )
        return Request.Builder()
            .url(url)
            .headers(headers)
            .build()
    }

    override suspend fun getPageResponse(page: MangaPage, client: OkHttpClient?): Response {
        val httpClient = client ?: loaderContext.httpClient
        val request = getPageRequest(page)
        return httpClient.newCall(request).execute()
    }

    override fun getHeaders(): Headers? {
        return parser.getRequestHeaders()
    }
}
