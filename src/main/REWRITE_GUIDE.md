# Tsuki API Compatibility & Clean-Room Relicensing Guide

This document classifies every file in the project to help you rewrite the codebase cleanly under the **Apache 2.0** license while preserving complete binary compatibility with compiled `.jar` plugins (such as **UMA**).

---

## Legend & Rules

* 🟢 **CAN FULLY REWRITE (Internal Logic & Utilities)**  
  * **What it means:** These files contain heavy algorithmic logic, networking, string manipulation, and parsing routines.  
  * **What you can do:** You can rewrite the entire code from scratch in your own style. You only need to keep the public function names/signatures if plugins import them directly.
* 🟡 **SEMI-RESTRICTED / ABI CONTRACT (Models, Enums, Interfaces)**  
  * **What it means:** These files define the JVM Binary Interface (ABI) that plugins like `UMA.jar` link against at runtime.  
  * **What you CANNOT change:** Package names (`tsuki.*`), class/interface names, property names, types, nullability, constructor parameter order, and `@JvmField` annotations.  
  * **What you CAN change:** Delete all original GPL docstrings/comments and write your own documentation. Any function bodies or helper calculations inside class bodies can be rewritten completely.
* 🔵 **EXTENSIBLE / ADDITIONS ALLOWED**  
  * You may add new helper functions, extension methods, or default implementations to any file without breaking compatibility.

---

## Detailed File Classification

### 1. Root Package (`src/main/kotlin/tsuki/`)

| File | Status | Guidelines |
| :--- | :---: | :--- |
| `MangaParser.kt` | 🟡 Semi-Restricted | Core interface. Keep all method signatures (`getList`, `getDetails`, `getPages`, etc.) and property definitions. Default method bodies can be rewritten. Remove original docstrings. |
| `MangaLoaderContext.kt` | 🟡 Semi-Restricted | Core environment interface passed to parsers. Method signatures must remain identical. |
| `MangaSourceParser.kt` | 🟡 Semi-Restricted | Interface. Preserve method declarations and property names. |
| `MangaParserAuthProvider.kt` | 🟡 Semi-Restricted | Auth provider interface. Preserve method signatures. |
| `InternalParsersApi.kt` | 🟡 Semi-Restricted | Annotation class. Keep name and annotation targets. |
| `ErrorMessages.kt` | 🟢 Can Fully Rewrite | Internal error message constants and formatting logic. |
| `Broken.kt` | 🟡 Semi-Restricted | Annotation or marker class. Keep class definition. |

---

### 2. Models & Data Classes (`src/main/kotlin/tsuki/model/`)

> **Important Rule for Models:** Plugins interact with compiled JVM bytecode. Do not alter constructor parameter order, property names, types, or `@JvmField` flags. Replace all docstrings with your own.

| File | Status | Guidelines |
| :--- | :---: | :--- |
| `Manga.kt` | 🟡 Semi-Restricted | Primary data class. Keep constructor properties, types, and `@JvmField` as-is. Methods inside the body (e.g., `findChapterById`) can have their logic rewritten. |
| `MangaChapter.kt` | 🟡 Semi-Restricted | Data class. Keep fields and constructor order intact. |
| `MangaPage.kt` | 🟡 Semi-Restricted | Data class. Keep fields and constructor order intact. |
| `MangaSource.kt` | 🟡 Semi-Restricted | Data class / interface for sources. Keep properties intact. |
| `MangaTag.kt` | 🟡 Semi-Restricted | Data class. Keep constructor parameters intact. |
| `MangaListFilter.kt` | 🟡 Semi-Restricted | Filter criteria object. Keep field names and types intact. |
| `MangaListFilterCapabilities.kt` | 🟡 Semi-Restricted | Capabilities flags. Keep boolean property names intact. |
| `MangaListFilterOptions.kt` | 🟡 Semi-Restricted | Filter options container. Keep properties intact. |
| `ContentRating.kt` | 🟡 Semi-Restricted | Enum. Keep existing enum entries (`SAFE`, `SUGGESTIVE`, `NSFW`, etc.). |
| `ContentType.kt` | 🟡 Semi-Restricted | Enum. Keep existing enum entries. |
| `Demographic.kt` | 🟡 Semi-Restricted | Enum. Keep existing enum entries. |
| `MangaState.kt` | 🟡 Semi-Restricted | Enum. Keep existing enum entries (`ONGOING`, `FINISHED`, etc.). |
| `SortOrder.kt` | 🟡 Semi-Restricted | Enum. Keep existing enum entries (`POPULARITY`, `UPDATED`, etc.). |
| `Constants.kt` | 🟡 Semi-Restricted | Public constants (e.g., `RATING_UNKNOWN`). Keep constant names and values. |
| `Favicon.kt` & `Favicons.kt` | 🟡 Semi-Restricted | Favicon model classes. Keep property signatures intact. |
| `WordSet.kt` | 🟢 Can Fully Rewrite | Set/lookup implementation. Keep public query methods; optimize internal storage freely. |

---

### 3. Search Models (`src/main/kotlin/tsuki/model/search/`)

| File | Status | Guidelines |
| :--- | :---: | :--- |
| `MangaSearchQuery.kt` | 🟡 Semi-Restricted | Search query model. Keep property names and types intact. |
| `MangaSearchQueryCapabilities.kt` | 🟡 Semi-Restricted | Search query capability flags. Keep properties intact. |
| `QueryCriteria.kt` | 🟡 Semi-Restricted | Search criteria interfaces/classes. Keep ABI signatures. |
| `SearchCapability.kt` | 🟡 Semi-Restricted | Capability enum/data classes. Keep names intact. |
| `SearchableField.kt` | 🟡 Semi-Restricted | Enum of searchable fields. Keep enum constants intact. |

---

### 4. Core Parser Base Classes (`src/main/kotlin/tsuki/core/`)

| File | Status | Guidelines |
| :--- | :---: | :--- |
| `AbstractMangaParser.kt` | 🟢 Can Fully Rewrite | Abstract base class. Keep the `abstract` and `open` method signatures, but completely rewrite the execution flow, lifecycle, and internal handling. |
| `FlexibleMangaParser.kt` | 🟢 Can Fully Rewrite | Helper template class. Rewrite internal parsing logic freely. |
| `FlexiblePagedMangaParser.kt` | 🟢 Can Fully Rewrite | Pagination loop and handler. Rewrite pagination algorithms freely. |
| `PagedMangaParser.kt` | 🟢 Can Fully Rewrite | Pagination template base. Rewrite pagination algorithms freely. |
| `SinglePageMangaParser.kt` | 🟢 Can Fully Rewrite | Non-paginated parser base. Rewrite parsing logic freely. |
| `MangaParserWrapper.kt` | 🟢 Can Fully Rewrite | Decorator/wrapper class. Rewrite delegation code freely. |

---

### 5. Network & HTTP (`src/main/kotlin/tsuki/network/`)

| File | Status | Guidelines |
| :--- | :---: | :--- |
| `WebClient.kt` | 🟡 Semi-Restricted | Interface. Keep public client methods so plugins can execute requests. |
| `OkHttpWebClient.kt` | 🟢 Can Fully Rewrite | Implementation of `WebClient`. Completely rewrite OkHttp configuration, interceptor chain, cache management, and response transformations. |
| `CloudFlareHelper.kt` | 🟢 Can Fully Rewrite | Cloudflare bypass and challenge resolution logic. Write your own interceptors and cookie handling. |
| `RateLimitHelper.kt` | 🟢 Can Fully Rewrite | Rate limiting coordination. Rewrite rate limiters using your own coroutine or queue logic. |
| `CommonHeaders.kt` | 🟢 Can Fully Rewrite | Static user agents and headers. Modify or update headers as needed. |
| `UserAgents.kt` | 🟢 Can Fully Rewrite | List of browser User-Agent strings. |
| `network/utils/RateLimiter.kt`| 🟢 Can Fully Rewrite | Token bucket / sliding window rate limiting algorithm. Write 100% your own implementation. |

---

### 6. Utilities & Helpers (`src/main/kotlin/tsuki/util/`)

> **All files in this section can have their internal code 100% replaced with your own clean algorithms.** As long as the public function signature matches, the logic inside belongs entirely to you.

| File | Status | What You Can Rewrite |
| :--- | :---: | :--- |
| `CryptoAES.kt` | 🟢 Can Fully Rewrite | AES decrypt/encrypt logic using standard Java `Cipher`. |
| `FaviconParser.kt` | 🟢 Can Fully Rewrite | HTML favicon scraping and link extraction algorithms. |
| `Jsoup.kt` | 🟢 Can Fully Rewrite | HTML traversal extension functions (`selectFirstOrThrow`, etc.). |
| `OkHttp.kt` | 🟢 Can Fully Rewrite | OkHttp coroutine awaiters and response body converters. |
| `RelatedMangaFinder.kt` | 🟢 Can Fully Rewrite | String similarity / fuzzy matching algorithms (e.g. Levenshtein). |
| `SearchQueryConverter.kt`| 🟢 Can Fully Rewrite | URL query string generator and parser algorithms. |
| `WebViewHelper.kt` | 🟢 Can Fully Rewrite | Android WebView interaction and cookie extraction routines. |
| `Chapters.kt` | 🟢 Can Fully Rewrite | Chapter numbering, float conversions, and sorting regexes. |
| `String.kt` | 🟢 Can Fully Rewrite | String extensions, sanitizers, slugifiers, and substring helpers. |
| `Number.kt` | 🟢 Can Fully Rewrite | Number parsing and float normalization utilities. |
| `Collection.kt` | 🟢 Can Fully Rewrite | Collection mapping and filtering helpers. |
| `CookieJar.kt` | 🟢 Can Fully Rewrite | In-memory and persistent cookie storage implementations. |
| `Coroutines.kt` | 🟢 Can Fully Rewrite | Coroutine dispatchers and scope management. |
| `ContinuationCallCallback.kt` | 🟢 Can Fully Rewrite | Callback to coroutine bridge. |
| `CSSBackground.kt` | 🟢 Can Fully Rewrite | Regex for extracting URLs from `background-image: url(...)`. |
| `Enum.kt` | 🟢 Can Fully Rewrite | Safe enum lookups by name / index. |
| `LinkResolver.kt` | 🟢 Can Fully Rewrite | Relative-to-absolute URL resolution algorithm. |
| `MangaParserEnv.kt` | 🟢 Can Fully Rewrite | Environment configuration helpers. |
| `MangaParserExt.kt` | 🟢 Can Fully Rewrite | Parser extension methods. |
| `MangaParsersUtils.kt` | 🟢 Can Fully Rewrite | General parser helper routines. |
| `Paginator.kt` | 🟢 Can Fully Rewrite | Pagination state machine and batching logic. |
| `Parse.kt` | 🟢 Can Fully Rewrite | General date/string parsing helpers. |
| `RateLimit.kt` | 🟢 Can Fully Rewrite | Rate limit helpers. |
| `Result.kt` | 🟢 Can Fully Rewrite | Success/failure result monad wrappers. |
| `Assert.kt` | 🟢 Can Fully Rewrite | Precondition check helpers. |

---

### 7. JSON Helpers (`src/main/kotlin/tsuki/util/json/`)

| File | Status | Guidelines |
| :--- | :---: | :--- |
| `NextJs.kt` | 🟢 Can Fully Rewrite | Extraction algorithm for `__NEXT_DATA__` JSON blobs from HTML. |
| `JsonExt.kt` | 🟢 Can Fully Rewrite | Extension functions for JSON parsing and navigation. |
| `EscapeUtils.kt` | 🟢 Can Fully Rewrite | Unicode and string escaping/unescaping routines. |
| `JSONArrayTypedIterator.kt` | 🟢 Can Fully Rewrite | Iterator wrappers for JSON arrays. |
| `JSONArrayTypedListWrapper.kt` | 🟢 Can Fully Rewrite | List interface implementation wrapping JSON arrays. |
| `JSONObjectTypedIterableWrapper.kt` | 🟢 Can Fully Rewrite | Iterable wrapper for JSON objects. |

---

### 8. Concurrency Helpers (`src/main/kotlin/tsuki/util/suspendlazy/`)

| File | Status | Guidelines |
| :--- | :---: | :--- |
| `SuspendLazy.kt` | 🟡 Semi-Restricted | Interface for lazy suspendable values. Keep interface signature. |
| `SuspendLazyImpl.kt` | 🟢 Can Fully Rewrite | Mutex-based double-checked locking implementation. |
| `SoftSuspendLazyImpl.kt` | 🟢 Can Fully Rewrite | Soft reference memory cache implementation. |

---

### 9. Exceptions (`src/main/kotlin/tsuki/exception/`)

| File | Status | Guidelines |
| :--- | :---: | :--- |
| `AuthRequiredException.kt` | 🟡 Semi-Restricted | Exception class. Keep class name and constructors. |
| `ContentUnavailableException.kt` | 🟡 Semi-Restricted | Exception class. Keep class name and constructors. |
| `GraphQLException.kt` | 🟡 Semi-Restricted | Exception class. Keep class name and constructors. |
| `InputRequiredException.kt` | 🟡 Semi-Restricted | Exception class. Keep class name and constructors. |
| `NotFoundException.kt` | 🟡 Semi-Restricted | Exception class. Keep class name and constructors. |
| `ParseException.kt` | 🟡 Semi-Restricted | Exception class. Keep class name and constructors. |
| `TooManyRequestExceptions.kt` | 🟡 Semi-Restricted | Exception class. Keep class name and constructors. |

---

### 10. Bitmap & Config (`src/main/kotlin/tsuki/bitmap/` & `tsuki/config/`)

| File | Status | Guidelines |
| :--- | :---: | :--- |
| `bitmap/Bitmap.kt` | 🟡 Semi-Restricted | Bitmap wrapper interface. Keep method signatures. |
| `bitmap/Rect.kt` | 🟡 Semi-Restricted | Rectangle coordinates data class. Keep fields (`left`, `top`, `right`, `bottom`). |
| `config/ConfigKey.kt` | 🟡 Semi-Restricted | Configuration key classes/types. Keep property types and names. |
| `config/MangaSourceConfig.kt` | 🟡 Semi-Restricted | Config store interface. Keep method signatures. |
