# usagi-kit 🐇

[![Release](https://img.shields.io/github/v/release/verizonkert/usagi-kit?color=7C4DFF&label=Release&style=flat-square)](https://github.com/verizonkert/usagi-kit/releases)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg?style=flat-square)](https://opensource.org/licenses/Apache-2.0)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0%2B-purple.svg?style=flat-square&logo=kotlin)](https://kotlinlang.org)
[![Platform](https://img.shields.io/badge/Platform-Android%20%7C%20JVM-green.svg?style=flat-square)](https://developer.android.com)

A high-performance, modular Kotlin core SDK for manga reader apps and dynamic extension plugins. Built for seamless discovery, searching, pagination, chapter reading, and runtime `.jar` plugin loading with dual compatibility for both **Usagi** and archived **Kotatsu** parsers.

---

## ✨ Features

- ⚡ **High Performance**: Native JVM/Android ART execution with zero reflection overhead during scraping and parsing.
- 🔌 **Dynamic Plugin Loading**: Load pre-compiled `.jar` source extensions at runtime using standard Android `DexClassLoader`.
- 🔄 **Dual Ecosystem Compatibility**:
  - **Modern Usagi / Tsuki (`tsuki.*`)**: Clean, forward-looking architecture for new plugins and readers.
  - **Archived Kotatsu (`org.koitharu.kotatsu.parsers.*`)**: 100% drop-in backward compatibility with hundreds of existing Kotatsu extension `.jar` files.
- 🌐 **Robust Networking**: Built-in helpers for Cloudflare bypass, User-Agent rotation, rate limiting, and automated cookie persistence.
- 🔍 **Rich Query & Filters**: Comprehensive multi-tag inclusion/exclusion, demographic filtering, content ratings, sorting orders, and keyword search capabilities.
- 🛡️ **Apache 2.0 Licensed**: Permissive open-source license free from GPL infection.

> 📖 **Want to build an extension?**  
> Check out the complete step-by-step tutorial: **[How to Create a Manga Extension Plugin](HOW_TO_CREATE_EXTENSION.md)**.

---

## 📦 Installation

### Gradle (Kotlin DSL)

1. Add the Maven repository to your root `settings.gradle.kts` (or `build.gradle.kts`):

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://raw.githubusercontent.com/verizonkert/usagi-kit/mvn-repo/") }
    }
}
```

2. Add the dependency to your module's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.verizonkert:usagi-kit:<version>")
}
```

### Maven

```xml
<dependency>
    <groupId>com.github.verizonkert</groupId>
    <artifactId>usagi-kit</artifactId>
    <version>&lt;version&gt;</version>
</dependency>
```

---

## 🚀 Quick Start

### 1. Implement a Manga Parser

Create a new source plugin by extending `PagedMangaParser` or `SinglePageMangaParser`:

```kotlin
package tsuki.site.en

import tsuki.MangaLoaderContext
import tsuki.MangaSourceParser
import tsuki.core.PagedMangaParser
import tsuki.model.*
import okhttp3.Request

@MangaSourceParser(
    name = "MY_MANGA_SITE",
    title = "My Manga Site",
    locale = "en"
)
class MyMangaParser(context: MangaLoaderContext) : PagedMangaParser(context, MangaParserSource.MY_MANGA_SITE) {

    override val domain: String = "example.com"

    override suspend fun getListPage(page: Int, filter: MangaListFilter): List<Manga> {
        val document = client.httpGet("https://$domain/browse?page=$page").parseHtml()
        return document.select(".manga-card").map { element ->
            Manga(
                id = generateUid(element.selectFirst("a")!!.attr("href")),
                title = element.selectFirst(".title")!!.text(),
                url = element.selectFirst("a")!!.attr("href"),
                publicUrl = "https://$domain" + element.selectFirst("a")!!.attr("href"),
                coverUrl = element.selectFirst("img")?.src(),
                source = source
            )
        }
    }

    override suspend fun getDetails(manga: Manga): Manga {
        val doc = client.httpGet(manga.publicUrl).parseHtml()
        val chapters = doc.select(".chapter-item").mapIndexed { index, el ->
            MangaChapter(
                id = generateUid(el.attr("href")),
                name = el.selectFirst(".name")?.text() ?: "Chapter $index",
                url = el.attr("href"),
                uploadDate = 0L,
                source = source
            )
        }
        return manga.copy(chapters = chapters)
    }

    override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
        val doc = client.httpGet("https://$domain" + chapter.url).parseHtml()
        return doc.select(".reader img").mapIndexed { i, el ->
            MangaPage(
                id = generateUid(chapter.url + i),
                url = el.src()!!,
                source = source
            )
        }
    }
}
```

---

### 2. Dynamically Load Plugins at Runtime

Load external `.jar` extensions dynamically in your Android app:

```kotlin
val loader = DexClassLoader(jarFile.absolutePath, optDir.absolutePath, null, context.classLoader)

// Works with both modern Tsuki and legacy Kotatsu plugin JARs!
val (factoryClass, enumClass) = try {
    loader.loadClass("tsuki.MangaParserFactoryKt") to loader.loadClass("tsuki.model.MangaParserSource")
} catch (_: ClassNotFoundException) {
    loader.loadClass("org.koitharu.kotatsu.parsers.MangaParserFactoryKt") to 
    loader.loadClass("org.koitharu.kotatsu.parsers.model.MangaParserSource")
}

val newParserMethod = factoryClass.methods.first { it.name == "newParser" }
val sourceEnum = java.lang.Enum.valueOf(enumClass.asSubclass(Enum::class.java), "MY_MANGA_SITE")
val parser = newParserMethod.invoke(null, sourceEnum, loaderContext) as tsuki.MangaParser
```

---

## 🧩 Compatibility Guidelines

To preserve clarity while maximizing compatibility across the ecosystem:

| Package Namespace | Usage | Description |
| :--- | :--- | :--- |
| **`tsuki.*`** |  **Recommended** | Always use for new reader apps, models, and plugins. |
| **`org.koitharu.kotatsu.parsers.*`** | ⚠️ **Legacy Bridge** | Built-in binary bridge for loading archived Kotatsu `.jar` extensions without modification. Marked `@Deprecated` with automatic IDE quick-fixes. |

---

## 🛠️ Included Tools & Samples

- **[tester/](file:///c:/Users/ASUS/Documents/Usagi-kit/tester)**: A full Jetpack Compose Android sample application showcasing:
  - Runtime `.jar` file picker and verification.
  - Interactive multi-source manga catalog browsing.
  - Live chapter pagination and page-by-page image reading.

---

## 📄 License

```
Copyright 2026 verizonkert & Contributors

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
