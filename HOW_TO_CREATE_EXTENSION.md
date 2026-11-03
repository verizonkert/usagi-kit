# How to Create a Manga Extension Plugin with `usagi-kit` 🧩

This comprehensive guide walks you step-by-step through creating, testing, and compiling a standalone `.jar` manga extension plugin using **`usagi-kit`**.

---

## Table of Contents
1. [Extension Architecture Overview](#1-extension-architecture-overview)
2. [Project Setup & Gradle Configuration](#2-project-setup--gradle-configuration)
3. [Choosing the Right Parser Base Class](#3-choosing-the-right-parser-base-class)
4. [Step-by-Step Implementation Example](#4-step-by-step-implementation-example)
   - [A. Parser Metadata & Class Declaration](#a-parser-metadata--class-declaration)
   - [B. Implementing Catalog Pagination (`getListPage`)](#b-implementing-catalog-pagination-getlistpage)
   - [C. Implementing Details & Chapter List (`getDetails`)](#c-implementing-details--chapter-list-getdetails)
   - [D. Implementing Page Image Extraction (`getPages`)](#d-implementing-page-image-extraction-getpages)
   - [E. Adding Filter & Tag Capabilities (`getFilterOptions`)](#e-adding-filter--tag-capabilities-getfilteroptions)
5. [Registering Sources with `MangaParserFactory`](#5-registering-sources-with-mangaparserfactory)
6. [Building & Dexing the Extension JAR](#6-building--dexing-the-extension-jar)
7. [Testing Your Extension in the Tester App](#7-testing-your-extension-in-the-tester-app)
8. [Best Practices & Security](#8-best-practices--security)

---

## 1. Extension Architecture Overview

Every manga plugin is an independent module compiled into an Android-compatible `.jar` containing a `classes.dex` file. At runtime, the reader app dynamically loads this `.jar` using `DexClassLoader`.

A plugin contains:
- **One or more Parser Classes**: Subclasses of `PagedMangaParser` or `SinglePageMangaParser`.
- **`MangaParserSource` Enum**: Enumerates the unique source identifiers provided by the plugin.
- **`MangaParserFactoryKt.newParser(...)`**: The factory function that the reader app invokes to instantiate your parser.

---

## 2. Project Setup & Gradle Configuration

Create a standard Kotlin JVM project with the Android `d8` dexing tool configured.

### `build.gradle.kts`
```kotlin
plugins {
    kotlin("jvm") version "2.0.21"
}

repositories {
    google()
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    // Compile against usagi-kit
    compileOnly("com.github.verizonkert:usagi-kit:1.0.6")
    
    // Core dependencies
    compileOnly("org.jsoup:jsoup:1.17.2")
    compileOnly("com.squareup.okhttp3:okhttp:5.3.2")
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
}

// Task to compile classes into an Android classes.dex and package into a JAR
tasks.register<Jar>("pluginJar") {
    archiveFileName.set("my-extension.jar")
    from(sourceSets.main.get().output)
    // Run Android D8 / Dexer here to embed classes.dex into the root of the JAR
}
```

> **Important:** All core libraries (`usagi-kit`, `okhttp`, `jsoup`, `coroutines`) must be declared as `compileOnly`. They are already provided by the host reader app at runtime, which keeps your plugin file size tiny (~15–50 KB).

---

## 3. Choosing the Right Parser Base Class

`usagi-kit` provides base classes tailored to how websites structure their catalog:

| Class | When to Use | Key Method to Implement |
| :--- | :--- | :--- |
| **`PagedMangaParser`** | Websites with page-based browsing (`?page=1`, `?page=2`). | `getListPage(page: Int, filter: MangaListFilter)` |
| **`SinglePageMangaParser`** | Sources that return their entire catalog or fetch all items in one call. | `getList(): List<Manga>` |
| **`FlexibleMangaParser`** | Advanced sources using offset-based pagination (`?offset=20&limit=20`) or infinite GraphQL cursors. | `getList(offset: Int, filter: MangaListFilter)` |

---

## 4. Step-by-Step Implementation Example

Let's implement a real-world scraper for a website: **`AsuraScans`**.

### A. Parser Metadata & Class Declaration

```kotlin
package tsuki.site.en

import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import tsuki.MangaLoaderContext
import tsuki.MangaSourceParser
import tsuki.core.PagedMangaParser
import tsuki.model.*
import tsuki.util.*

@MangaSourceParser(
    name = "ASURA_SCANS",
    title = "Asura Scans",
    locale = "en"
)
class AsuraScansParser(context: MangaLoaderContext) : PagedMangaParser(context, MangaParserSource.ASURA_SCANS) {

    override val domain: String = "asuracomic.net"

    // Default headers required by the source
    override fun getRequestHeaders(): Headers = Headers.Builder()
        .add("User-Agent", UserAgents.CHROME_DESKTOP)
        .add("Referer", "https://$domain/")
        .build()

    // Supported sort orders
    override val availableSortOrders: Set<SortOrder> = setOf(
        SortOrder.UPDATED,
        SortOrder.POPULARITY,
        SortOrder.NEWEST,
        SortOrder.ALPHABETICAL
    )
```

---

### B. Implementing Catalog Pagination (`getListPage`)

Fetches a specific page of results based on active filters and search queries:

```kotlin
    override suspend fun getListPage(page: Int, filter: MangaListFilter): List<Manga> {
        val url = "https://$domain/series".toHttpUrl().newBuilder().apply {
            addQueryParameter("page", page.toString())

            // Handle keyword search
            filter.query?.let { keyword ->
                addQueryParameter("name", keyword)
            }

            // Handle sorting
            when (filter.sortOrder) {
                SortOrder.POPULARITY -> addQueryParameter("order", "popular")
                SortOrder.NEWEST -> addQueryParameter("order", "latest")
                SortOrder.ALPHABETICAL -> addQueryParameter("order", "title")
                else -> addQueryParameter("order", "update")
            }

            // Handle tag/genre filtering
            filter.tags.forEach { tag ->
                addQueryParameter("genres[]", tag.key)
            }
        }.build()

        // Fetch HTML and parse with Jsoup
        val doc: Document = client.httpGet(url).parseHtml()

        return doc.select(".grid > div").mapNotNull { el ->
            val link = el.selectFirst("a[href*=/series/]") ?: return@mapNotNull null
            val titleEl = el.selectFirst("span.block.font-bold") ?: return@mapNotNull null
            val coverEl = el.selectFirst("img")

            val mangaUrl = link.attr("href")

            Manga(
                id = generateUid(mangaUrl), // Generates stable 64-bit ID
                title = titleEl.text().trim(),
                url = mangaUrl,
                publicUrl = "https://$domain$mangaUrl",
                coverUrl = coverEl?.src() ?: "",
                source = source,
                rating = RATING_UNKNOWN
            )
        }
    }
```

---

### C. Implementing Details & Chapter List (`getDetails`)

Parses the manga's description, authors, genres, status, and list of chapters:

```kotlin
    override suspend fun getDetails(manga: Manga): Manga {
        val doc: Document = client.httpGet(manga.publicUrl).parseHtml()

        val description = doc.selectFirst("span.font-medium.text-sm")?.text()?.trim()
        val author = doc.selectFirst("h3:contains(Author) + h3")?.text()?.trim()
        val artist = doc.selectFirst("h3:contains(Artist) + h3")?.text()?.trim()

        val statusText = doc.selectFirst("h3:contains(Status) + h3")?.text()?.lowercase()
        val state = when {
            statusText?.contains("ongoing") == true -> MangaState.ONGOING
            statusText?.contains("completed") == true -> MangaState.FINISHED
            else -> MangaState.UNKNOWN
        }

        val tags = doc.select("div.flex.flex-wrap.gap-1 a").mapNotNull {
            MangaTag(
                key = it.attr("href").substringAfter("genre="),
                title = it.text().trim(),
                source = source
            )
        }.toSet()

        // Parse chapters (Newest first or Oldest first)
        val chapters = doc.select(".scrollbar-thumb-muted div.flex.justify-between").mapIndexed { index, el ->
            val link = el.selectFirst("a")!!
            val chapterUrl = link.attr("href")
            val name = link.selectFirst("h3")?.text()?.trim() ?: "Chapter $index"
            val dateStr = el.selectFirst("h4")?.text()?.trim()

            MangaChapter(
                id = generateUid(chapterUrl),
                name = name,
                url = chapterUrl,
                uploadDate = parseChapterDate(dateStr),
                source = source
            )
        }

        return manga.copy(
            description = description,
            author = author,
            artist = artist,
            state = state,
            tags = tags,
            chapters = chapters
        )
    }
```

---

### D. Implementing Page Image Extraction (`getPages`)

Extracts the high-resolution image URLs for a chapter:

```kotlin
    override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
        val chapterUrl = if (chapter.url.startsWith("http")) chapter.url else "https://$domain${chapter.url}"
        val doc: Document = client.httpGet(chapterUrl).parseHtml()

        return doc.select("div.w-full.mx-auto.center img").mapIndexedNotNull { index, el ->
            val imageUrl = el.src() ?: return@mapIndexedNotNull null
            MangaPage(
                id = generateUid("$chapterUrl/$index"),
                url = imageUrl,
                source = source
            )
        }
    }
```

---

### E. Adding Filter & Tag Capabilities (`getFilterOptions`)

Exposes website genres and tags so user apps can display interactive filter menus:

```kotlin
    override suspend fun getFilterOptions(): MangaListFilterOptions {
        val doc = client.httpGet("https://$domain/series").parseHtml()
        val genres = doc.select("select[name='genres'] option").mapNotNull { option ->
            val key = option.attr("value")
            if (key.isBlank()) return@mapNotNull null
            MangaTag(
                key = key,
                title = option.text().trim(),
                source = source
            )
        }.toSet()

        return MangaListFilterOptions(
            availableTags = genres,
            availableSortOrders = availableSortOrders
        )
    }
}
```

---

## 5. Registering Sources with `MangaParserFactory`

Every extension module must provide a source enum and the `newParser` factory function:

### `MangaParserSource.kt`
```kotlin
package tsuki.model

enum class MangaParserSource {
    ASURA_SCANS
}
```

### `MangaParserFactory.kt`
```kotlin
package tsuki

import tsuki.model.MangaParserSource
import tsuki.site.en.AsuraScansParser

fun newParser(source: MangaParserSource, context: MangaLoaderContext): MangaParser {
    return when (source) {
        MangaParserSource.ASURA_SCANS -> AsuraScansParser(context)
    }
}
```

---

## 6. Building & Dexing the Extension JAR

Android's ART runtime cannot execute desktop `.class` bytecode directly. You must convert the compiled classes into Android's `classes.dex` using the `d8` tool (from Android SDK `build-tools`).

### Automated Script / Gradle Step:
```bash
# 1. Compile Kotlin sources to .class files
./gradlew classes

# 2. Run Android d8 to produce classes.dex
$ANDROID_HOME/build-tools/34.0.0/d8 build/classes/kotlin/main/**/*.class --output build/dex/

# 3. Create the final plugin JAR containing classes.dex at the root
cd build/dex && zip -9 ../libs/asurascans-plugin.jar classes.dex
```

---

## 7. Testing Your Extension in the Tester App

You can verify and interactively test your new `.jar` plugin using the included `tester/` Android app:

1. Launch the **Usagi Tester App** on your device or emulator:
   ```bash
   ./gradlew :app:installDebug
   ```
2. Tap the **"📁 Load JAR"** button in the top bar.
3. Select your compiled `asurascans-plugin.jar`.
4. Verify:
   - ✅ Sources load and populate in the dropdown.
   - ✅ Catalog renders covers, titles, and ratings.
   - ✅ Keyword search and sorting work.
   - ✅ Chapters open and pages render smoothly in the built-in reader.

---

## 8. Best Practices & Security

- **Respect Rate Limits:** Use the built-in `RateLimiter` or `.rateLimit(permits = 3, period = 1.seconds)` to avoid getting the user's IP temporarily banned.
- **Stable IDs:** Always use `generateUid(url)` so manga IDs and chapter IDs remain stable across app launches and caching layers.
- **Fail Gracefully:** Use `mapNotNull` and `.parseHtmlOrNull()` to handle layout changes or broken entries without crashing the reader.
- **Zero Heavy Dependencies:** Never bundle your own copies of `OkHttp` or `Jsoup` inside the plugin `.jar`; keep them `compileOnly`.
