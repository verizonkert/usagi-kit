# UsagiKit Usage Guide 📖

This guide explains how to integrate and use **UsagiKit** to dynamically load and query manga extension plugins.

---

## 1. Requirements

- Android `minSdk`: **21+**
- `compileSdk`: **34+** (Recommended 36)
- Java version: **Java 17**
- Enable Core Library Desugaring (if supporting Android API < 26).

---

## 2. Initialization

Initialize `UsagiKit` in your `Application` class before executing any extension calls:

```kotlin
import android.app.Application
import org.draken.usagikit.UsagiKit

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize with Application context
        UsagiKit.initialize(this)
    }
}
```

---

## 3. Loading Plugins

Usagi plugins are distributed as compiled `.jar` files containing `.dex` code and an extension manifest/factory.

### Option A: Load a Single Plugin File
```kotlin
import java.io.File
import org.draken.usagikit.UsagiKit
import org.draken.usagikit.model.UsagiExtension

val pluginFile = File(context.filesDir, "plugins/asura.jar")

try {
    val extension: UsagiExtension = UsagiKit.loadExtension(pluginFile)
    println("Loaded Extension: ${extension.name} v${extension.versionName}")
    
    extension.sources.forEach { source ->
        println(" - Available Source: ${source.name} [${source.lang}]")
    }
} catch (e: Exception) {
    e.printStackTrace()
}
```

### Option B: Load All Plugins from a Directory
```kotlin
val pluginsDir = File(context.filesDir, "plugins")
val extensions: List<UsagiExtension> = UsagiKit.loadExtensionsFromDir(pluginsDir)

println("Total loaded extensions: ${extensions.size}")
```

### Listing Loaded Sources
```kotlin
// Get all registered sources
val allSources = UsagiKit.getAllSources()

// Look up a specific source
val source = UsagiKit.getSourceByName("Asura Scans")
```

---

## 4. Querying Manga Content

All querying operations run asynchronously using Kotlin Coroutines via `UsagiKit` or directly through `UsagiMangaBridge`.

### Fetching Popular Manga
```kotlin
lifecycleScope.launch {
    try {
        val popularList = UsagiKit.getPopularManga(
            sourceName = "Asura Scans",
            page = 1
        )
        popularList.forEach { manga ->
            println("${manga.title} - ${manga.url}")
        }
    } catch (e: Exception) {
        // Handle network or parsing error
    }
}
```

### Fetching Latest Updates
```kotlin
lifecycleScope.launch {
    val latestManga = UsagiKit.getLatestManga(
        sourceName = "Asura Scans",
        page = 1
    )
}
```

### Searching Manga
```kotlin
lifecycleScope.launch {
    val searchResults = UsagiKit.searchManga(
        sourceName = "Asura Scans",
        query = "Solo Leveling",
        page = 1
    )
}
```

---

## 5. Manga Details & Chapters

Retrieve the full manga description, genres, status, and chapter list:

```kotlin
lifecycleScope.launch {
    val details: Manga = UsagiKit.getDetails(
        sourceName = "Asura Scans",
        manga = selectedManga
    )
    
    val chapters: List<MangaChapter> = details.chapters.orEmpty()
    chapters.forEach { ch ->
        println("${ch.name} (Number: ${ch.number}) -> ${ch.url}")
    }
}
```

---

## 6. Resolving Pages & Images

Fetch the image URLs for a specific chapter:

```kotlin
lifecycleScope.launch {
    val pages: List<MangaPage> = UsagiKit.getPages(
        sourceName = "Asura Scans",
        chapter = selectedChapter
    )
    
    pages.forEach { page ->
        // Direct image URL
        val imageUrl = UsagiKit.getPageUrl("Asura Scans", page)
        println("Page ${page.index}: $imageUrl")
    }
}
```

### Handling Custom Headers (Anti-Hotlinking / Cloudflare)
Some sources require specific headers (like `Referer` or `User-Agent`) to load chapter images:

```kotlin
val headers: Map<String, String> = UsagiKit.getHeaders("Asura Scans")

// When using Coil, Glide, or OkHttp:
val request = okhttp3.Request.Builder()
    .url(imageUrl)
    .apply {
        headers.forEach { (k, v) -> addHeader(k, v) }
    }
    .build()
```

---

## 7. Working with `UsagiMangaBridge` Directly

If you prefer operating directly on a bridge instance:

```kotlin
val bridge: UsagiMangaBridge? = UsagiKit.getBridge("Asura Scans")

if (bridge != null) {
    val mangaList = bridge.getPopularManga(page = 1)
    val chapterPages = bridge.getPages(chapter)
}
```

---

## 8. Unloading Extensions

When cleaning up or removing a plugin:

```kotlin
// Unload a specific extension
UsagiKit.unloadExtension(extensionPackageName)

// Or reset all cached extensions
UsagiKit.clearAll()
```

